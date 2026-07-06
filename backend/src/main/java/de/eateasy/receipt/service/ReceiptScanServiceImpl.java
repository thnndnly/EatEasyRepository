package de.eateasy.receipt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eateasy.common.exception.BadRequestException;
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
 * Ingredient-Matching. Bewusst fehlertolerant: schlaegt die Strukturierung
 * fehl, kommt der Rohtext mit leerer Item-Liste zurueck statt eines Fehlers —
 * das UI zeigt dann den Text und der User kann manuell anlegen.
 */
@ApplicationScoped
public class ReceiptScanServiceImpl implements ReceiptScanService {

    private static final Logger LOG = Logger.getLogger(ReceiptScanServiceImpl.class);

    private static final int MAX_ITEMS = 40;
    private static final int AMOUNT_SCALE = 2;

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

        String rawText = ocrClient.extractText(imageBytes, filename);
        if (rawText == null || rawText.isBlank()) {
            throw new BadRequestException(
                "Auf dem Bild wurde kein Text erkannt — bitte schaerfer fotografieren");
        }

        List<ReceiptItemDto> items = structureWithOllama(rawText);
        return new ReceiptScanResponse(rawText, items);
    }

    // --- LLM-Strukturierung ------------------------------------------------

    private List<ReceiptItemDto> structureWithOllama(String rawText) {
        try {
            OllamaGenerateResponse response = ollamaClient.generate(
                OllamaGenerateRequest.of(ollamaModel, buildPrompt(rawText)));
            if (response == null || response.response() == null || response.response().isBlank()) {
                LOG.warn("Ollama lieferte leere Antwort fuer Beleg-Strukturierung");
                return List.of();
            }
            return parseItems(response.response());
        } catch (Exception ex) {
            LOG.warnf(ex, "Beleg-Strukturierung fehlgeschlagen — liefere nur Rohtext");
            return List.of();
        }
    }

    private static String buildPrompt(String rawText) {
        // replace statt String.formatted: der Bon-Text (und der Prompt selbst,
        // "3,5%") enthaelt literale %-Zeichen, die formatted als
        // Format-Spezifizierer interpretieren wuerde.
        return """
            Du extrahierst Lebensmittel aus einem deutschen Kassenbon (OCR-Text, \
            kann Fehler enthalten). Ignoriere Preise, Pfand, Rabatte, Summen und \
            Nicht-Lebensmittel. Normalisiere Produktnamen auf die Grundzutat \
            (z. B. "Bio Vollmilch 3,5%" -> "Milch"). Schaetze Menge und Einheit; \
            erlaubte Einheiten: GRAM, ML, PIECE. Wenn unklar: 1 PIECE.
            Kassenbon-Text:
            {BON_TEXT}
            Antworte AUSSCHLIESSLICH mit JSON im Format: \
            [{"name":"<Zutat>","amount":<Zahl>,"unit":"GRAM|ML|PIECE"}]. \
            Keine Erklaerungen ausserhalb des JSON.
            """.replace("{BON_TEXT}", rawText);
    }

    private List<ReceiptItemDto> parseItems(String body) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(body));
            JsonNode array = root.isArray() ? root : findFirstArray(root);
            if (array == null || !array.isArray()) {
                LOG.warnf("Ollama-Antwort enthaelt kein Array: %s", body);
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
        Unit unit = UnitParser.parse(unitToken, Unit.PIECE).unit();

        UUID ingredientId = matchIngredient(name);
        return new ReceiptItemDto(name, amount, unit, ingredientId);
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
            LOG.debugf(ex, "Ingredient-Matching fuer '%s' fehlgeschlagen", name);
        }
        return null;
    }

    // --- JSON-Toleranz-Helpers (gleiche Formen wie beim Suggestion-Parser) --

    /** Schneidet Prosa vor/nach dem JSON ab, falls das Modell doch erklaert. */
    private static String extractJson(String body) {
        int arrayStart = body.indexOf('[');
        int arrayEnd = body.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return body.substring(arrayStart, arrayEnd + 1);
        }
        return body;
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
