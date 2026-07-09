package de.eateasy.receipt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eateasy.common.exception.BadRequestException;
import de.eateasy.common.exception.ServiceUnavailableException;
import de.eateasy.common.units.Unit;
import de.eateasy.common.units.UnitParser;
import de.eateasy.household.service.HouseholdService;
import de.eateasy.ingredient.dto.IngredientDto;
import de.eateasy.ingredient.service.IngredientService;
import de.eateasy.receipt.client.OcrClient;
import de.eateasy.receipt.dto.ReceiptItemDto;
import de.eateasy.receipt.dto.ReceiptScanResponse;
import de.eateasy.suggestion.client.OllamaClient;
import de.eateasy.suggestion.client.OllamaGenerateRequest;
import de.eateasy.suggestion.client.OllamaGenerateResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pipeline des Beleg-Scanners (Phase 11): OCR → LLM-Strukturierung →
 * Ingredient-Matching. Bewusst fehlertolerant: schlägt die Strukturierung
 * fehl, kommt der Rohtext mit leerer Item-Liste zurück statt eines Fehlers —
 * das UI zeigt dann den Text und der User kann manuell anlegen.
 */
@ApplicationScoped
public class ReceiptScanServiceImpl implements ReceiptScanService {

    private static final Logger LOG = Logger.getLogger(ReceiptScanServiceImpl.class);

    private static final int MAX_ITEMS = 40;
    private static final int AMOUNT_SCALE = 2;

    /**
     * Obergrenze für den OCR-Text, der in den Ollama-Prompt eingebettet wird.
     * Ein dichtes/adversariales Bild könnte sonst einen riesigen Prompt
     * erzeugen und die geteilte, sequentielle Ollama-Instanz (auch von der
     * Smart-Suggestion genutzt) blockieren. Ein realer Kassenbon liegt weit
     * darunter.
     */
    private static final int MAX_PROMPT_TEXT_CHARS = 8_000;

    private final OcrClient ocrClient;
    private final OllamaClient ollamaClient;
    private final IngredientService ingredientService;
    private final HouseholdService householdService;
    private final ObjectMapper objectMapper;
    private final String ollamaModel;

    public ReceiptScanServiceImpl(
        OcrClient ocrClient,
        OllamaClient ollamaClient,
        IngredientService ingredientService,
        HouseholdService householdService,
        ObjectMapper objectMapper,
        @ConfigProperty(name = "ollama.model", defaultValue = "llama3") String ollamaModel
    ) {
        this.ocrClient = ocrClient;
        this.ollamaClient = ollamaClient;
        this.ingredientService = ingredientService;
        this.householdService = householdService;
        this.objectMapper = objectMapper;
        this.ollamaModel = ollamaModel;
    }

    @Override
    public ReceiptScanResponse scan(UUID userId, UUID householdId,
                                    byte[] imageBytes, String filename) {
        householdService.assertMember(userId, householdId);

        String rawText = extractText(imageBytes, filename);
        if (rawText == null || rawText.isBlank()) {
            throw new BadRequestException(
                "Auf dem Bild wurde kein Text erkannt — bitte schärfer fotografieren");
        }

        List<ReceiptItemDto> items = structureWithOllama(rawText);
        return new ReceiptScanResponse(rawText, items);
    }

    // --- OCR ---------------------------------------------------------------

    /**
     * Kapselt den OCR-Aufruf. Fällt der Tesseract-Dienst aus (Timeout,
     * Connection refused, non-2xx), wirft {@link OcrClient} eine
     * {@link RuntimeException} — die wird hier gefangen, serverseitig mit
     * Detail geloggt und als {@link ServiceUnavailableException} (HTTP 503,
     * generische Nachricht) übersetzt, statt als unbehandelte 500 mit
     * internen Details beim Client zu landen.
     */
    private String extractText(byte[] imageBytes, String filename) {
        try {
            return ocrClient.extractText(imageBytes, filename);
        } catch (RuntimeException ex) {
            LOG.errorf(ex, "OCR-Aufruf fehlgeschlagen für Datei '%s'", filename);
            throw new ServiceUnavailableException(
                "Texterkennung ist gerade nicht verfügbar — bitte später erneut versuchen");
        }
    }

    // --- LLM-Strukturierung ------------------------------------------------

    private List<ReceiptItemDto> structureWithOllama(String rawText) {
        try {
            OllamaGenerateResponse response = ollamaClient.generate(
                OllamaGenerateRequest.of(ollamaModel, buildPrompt(rawText)));
            if (response == null || response.response() == null || response.response().isBlank()) {
                LOG.warn("Ollama lieferte leere Antwort für Beleg-Strukturierung");
                return List.of();
            }
            return parseItems(response.response());
        } catch (Exception ex) {
            LOG.warnf(ex, "Beleg-Strukturierung fehlgeschlagen — liefere nur Rohtext");
            return List.of();
        }
    }

    private static String buildPrompt(String rawText) {
        // Rohtext vor dem Einbetten kappen: ein dichtes/adversariales Bild
        // könnte sonst einen riesigen Prompt erzeugen und die geteilte,
        // sequentielle Ollama-Instanz blockieren.
        String promptText = rawText.length() > MAX_PROMPT_TEXT_CHARS
            ? rawText.substring(0, MAX_PROMPT_TEXT_CHARS)
            : rawText;
        // replace statt String.formatted: der Bon-Text (und der Prompt selbst,
        // "3,5%") enthält literale %-Zeichen, die formatted als
        // Format-Spezifizierer interpretieren würde.
        return """
            Du extrahierst Lebensmittel aus einem deutschen Kassenbon (OCR-Text, \
            kann Fehler enthalten). Ignoriere Preise, Pfand, Rabatte, Summen und \
            Nicht-Lebensmittel. Normalisiere Produktnamen auf die Grundzutat \
            (z. B. "Bio Vollmilch 3,5%" -> "Milch"). Schätze Menge und Einheit; \
            erlaubte Einheiten: GRAM, ML, PIECE. Wenn unklar: 1 PIECE.
            Kassenbon-Text:
            {BON_TEXT}
            Antworte AUSSCHLIESSLICH mit JSON im Format: \
            [{"name":"<Zutat>","amount":<Zahl>,"unit":"GRAM|ML|PIECE"}]. \
            Keine Erklärungen ausserhalb des JSON.
            """.replace("{BON_TEXT}", promptText);
    }

    private List<ReceiptItemDto> parseItems(String body) {
        try {
            JsonNode array = findItemsArray(body);
            if (array == null) {
                LOG.warnf("Ollama-Antwort enthält kein Item-Array: %s", body);
                return List.of();
            }

            List<ReceiptItemDto> items = new ArrayList<>();
            for (JsonNode node : array) {
                ReceiptItemDto item = toItem(node);
                if (item != null) {
                    items.add(item);
                }
                if (items.size() >= MAX_ITEMS) {
                    break;
                }
            }
            return items;
        } catch (Exception ex) {
            LOG.warnf(ex, "Beleg-Items nicht parsbar: %s", body);
            return List.of();
        }
    }

    private ReceiptItemDto toItem(JsonNode node) {
        JsonNode nameNode = node.get("name");
        if (nameNode == null || !nameNode.isTextual() || nameNode.asText().isBlank()) {
            return null;
        }
        String name = nameNode.asText().trim();

        BigDecimal amount = node.hasNonNull("amount") && node.get("amount").isNumber()
            ? node.get("amount").decimalValue().setScale(AMOUNT_SCALE, RoundingMode.HALF_UP)
            : BigDecimal.ONE;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            amount = BigDecimal.ONE;
        }

        String unitToken = node.hasNonNull("unit") ? node.get("unit").asText() : null;
        UnitParser.UnitParseResult unitResult = UnitParser.parse(unitToken, Unit.PIECE);
        if (unitResult.multiplier() != 1.0) {
            amount = amount.multiply(BigDecimal.valueOf(unitResult.multiplier()))
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        }

        UUID ingredientId = matchIngredient(name);
        return new ReceiptItemDto(name, amount, unitResult.unit(), ingredientId);
    }

    /** Exakter (case-insensitiver) Match gegen den globalen Zutaten-Pool. */
    private UUID matchIngredient(String name) {
        try {
            for (IngredientDto candidate : ingredientService.search(name, 5)) {
                if (candidate.name().equalsIgnoreCase(name)) {
                    return candidate.id();
                }
            }
        } catch (Exception ex) {
            LOG.debugf(ex, "Ingredient-Matching für '%s' fehlgeschlagen", name);
        }
        return null;
    }

    // --- JSON-Toleranz-Helpers (gleiche Formen wie beim Suggestion-Parser) --

    /**
     * Sucht das Item-Array in der LLM-Antwort: erst die ganze Antwort parsen
     * (direktes Array oder Objekt-Wrapper wie {@code {"items":[...]}}),
     * danach jeden {@code '['}-Kandidaten einzeln. Naives Slicen vom ersten
     * {@code '['} bis zum letzten {@code ']'} reicht nicht — Prosa um das
     * JSON kann selbst eckige Klammern enthalten ("Hinweis [OCR]: ...").
     */
    private JsonNode findItemsArray(String body) {
        JsonNode array = tryParseItemsArray(body);
        for (int i = body.indexOf('['); array == null && i >= 0; i = body.indexOf('[', i + 1)) {
            array = tryParseItemsArray(body.substring(i));
        }
        return array;
    }

    /** Liefert das Array nur, wenn der Text parsebar ist und Objekte enthält. */
    private JsonNode tryParseItemsArray(String text) {
        try {
            JsonNode root = objectMapper.readTree(text);
            JsonNode array = root.isArray() ? root : findFirstArray(root);
            if (array == null || (!array.isEmpty() && !array.get(0).isObject())) {
                return null;
            }
            return array;
        } catch (Exception ex) {
            return null;
        }
    }

    private static JsonNode findFirstArray(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isArray()) {
            return node;
        }
        if (node.isObject()) {
            for (JsonNode child : node) {
                JsonNode found = findFirstArray(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
