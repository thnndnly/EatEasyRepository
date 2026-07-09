package de.eateasy.suggestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;
import de.eateasy.household.service.HouseholdService;
import de.eateasy.ingredient.dto.IngredientDto;
import de.eateasy.ingredient.service.IngredientService;
import de.eateasy.pantry.service.PantryService;
import de.eateasy.recipe.dto.RecipeDto;
import de.eateasy.recipe.dto.RecipeFilter;
import de.eateasy.recipe.dto.RecipeIngredientView;
import de.eateasy.recipe.dto.RecipeMiniDto;
import de.eateasy.recipe.service.RecipeService;
import de.eateasy.suggestion.client.OllamaClient;
import de.eateasy.suggestion.client.OllamaGenerateRequest;
import de.eateasy.suggestion.client.OllamaGenerateResponse;
import de.eateasy.suggestion.dto.SuggestionDto;
import de.eateasy.suggestion.dto.SuggestionResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class SmartSuggestionServiceImpl implements SmartSuggestionService {

    private static final Logger LOG = Logger.getLogger(SmartSuggestionServiceImpl.class);

    /** Mindest-Abdeckung, damit ein Rezept als Kandidat für Ollama betrachtet wird. */
    private static final double COVERAGE_THRESHOLD = 0.5;

    /** Wie viele Top-Kandidaten wir an Ollama schicken (vermeidet Token-Explosion). */
    private static final int CANDIDATE_LIMIT = 10;

    private final RecipeService recipeService;
    private final PantryService pantryService;
    private final IngredientService ingredientService;
    private final HouseholdService householdService;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;
    private final String ollamaModel;

    public SmartSuggestionServiceImpl(
        RecipeService recipeService,
        PantryService pantryService,
        IngredientService ingredientService,
        HouseholdService householdService,
        OllamaClient ollamaClient,
        ObjectMapper objectMapper,
        @ConfigProperty(name = "ollama.model", defaultValue = "llama3") String ollamaModel
    ) {
        this.recipeService = recipeService;
        this.pantryService = pantryService;
        this.ingredientService = ingredientService;
        this.householdService = householdService;
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
        this.ollamaModel = ollamaModel;
    }

    @Override
    @Transactional
    public SuggestionResponse suggest(UUID userId, UUID householdId, int numSuggestions) {
        householdService.assertMember(userId, householdId);

        List<RecipeDto> recipes = recipeService.list(
            userId, new RecipeFilter(null, List.of(), null, false));
        if (recipes.isEmpty()) {
            return new SuggestionResponse(true, List.of());
        }

        Set<UUID> pantryIds = pantryService.getInventory(householdId).keySet();
        if (pantryIds.isEmpty()) {
            return new SuggestionResponse(true, List.of());
        }

        Map<UUID, List<RecipeIngredientView>> ingredientsByRecipe =
            recipeService.getIngredientsByRecipeIds(
                recipes.stream().map(RecipeDto::id).toList());

        Map<UUID, Double> coverage = CoverageCalculator.compute(ingredientsByRecipe, pantryIds);

        List<Candidate> candidates = recipes.stream()
            .map(r -> new Candidate(r, coverage.getOrDefault(r.id(), 0.0)))
            .filter(c -> c.coverage >= COVERAGE_THRESHOLD)
            .sorted(Comparator.comparingDouble((Candidate c) -> c.coverage).reversed())
            .limit(CANDIDATE_LIMIT)
            .toList();

        if (candidates.isEmpty()) {
            return new SuggestionResponse(true, List.of());
        }

        OllamaOutcome outcome = askOllama(candidates, pantryIds, ingredientsByRecipe);

        List<SuggestionDto> suggestions = candidates.stream()
            .limit(numSuggestions)
            .map(c -> new SuggestionDto(
                toMini(c.recipe),
                outcome.reasons().get(c.recipe.id()),
                c.coverage))
            .toList();
        return new SuggestionResponse(outcome.aiAvailable(), suggestions);
    }

    private static RecipeMiniDto toMini(RecipeDto dto) {
        return new RecipeMiniDto(
            dto.id(),
            dto.title(),
            dto.servings(),
            dto.prepMinutes(),
            dto.dietTags() == null ? List.of() : dto.dietTags());
    }

    // --- Ollama ----------------------------------------------------------

    /** Ergebnis der KI-Stufe: ob Ollama erreichbar war + die Begründungen. */
    private record OllamaOutcome(boolean aiAvailable, Map<UUID, String> reasons) {
    }

    private OllamaOutcome askOllama(
        List<Candidate> candidates,
        Set<UUID> pantryIds,
        Map<UUID, List<RecipeIngredientView>> ingredientsByRecipe
    ) {
        try {
            Map<UUID, IngredientDto> nameLookup = collectIngredientNames(
                candidates, pantryIds, ingredientsByRecipe);
            String prompt = buildPrompt(candidates, pantryIds, ingredientsByRecipe, nameLookup);
            OllamaGenerateResponse response = ollamaClient.generate(
                OllamaGenerateRequest.of(ollamaModel, prompt));
            if (response == null || response.response() == null || response.response().isBlank()) {
                // Leere Antwort = KI faktisch nicht nutzbar (z. B. Modell fehlt).
                LOG.errorf("Ollama lieferte leere Antwort — Modell '%s' verfügbar?", ollamaModel);
                return new OllamaOutcome(false, Map.of());
            }
            LOG.debugf("Ollama raw response: %s", response.response());
            Map<UUID, String> parsed = parseOllamaResponse(response.response());
            if (parsed.isEmpty()) {
                LOG.warnf("Ollama-Antwort parsed zu leerer Map. Raw: %s", response.response());
                return new OllamaOutcome(false, Map.of());
            }
            // Ollama hat geantwortet und lieferte mind. eine Begründung.
            return new OllamaOutcome(true, parsed);
        } catch (Exception ex) {
            LOG.errorf(ex, "Ollama-Aufruf fehlgeschlagen — falle zurück auf Coverage-Reihenfolge");
            return new OllamaOutcome(false, Map.of());
        }
    }

    private Map<UUID, IngredientDto> collectIngredientNames(
        List<Candidate> candidates,
        Set<UUID> pantryIds,
        Map<UUID, List<RecipeIngredientView>> ingredientsByRecipe
    ) {
        Set<UUID> needed = new HashSet<>(pantryIds);
        for (Candidate c : candidates) {
            for (RecipeIngredientView ing : ingredientsByRecipe.getOrDefault(c.recipe.id(), List.of())) {
                needed.add(ing.ingredientId());
            }
        }
        return ingredientService.getByIds(needed);
    }

    private String buildPrompt(
        List<Candidate> candidates,
        Set<UUID> pantryIds,
        Map<UUID, List<RecipeIngredientView>> ingredientsByRecipe,
        Map<UUID, IngredientDto> names
    ) {
        StringBuilder pantryList = new StringBuilder();
        for (UUID id : pantryIds) {
            IngredientDto dto = names.get(id);
            if (dto != null) {
                pantryList.append("- ").append(dto.name()).append('\n');
            }
        }

        StringBuilder recipeList = new StringBuilder();
        for (Candidate c : candidates) {
            recipeList.append("- id=").append(c.recipe.id())
                .append(", titel=\"").append(c.recipe.title()).append("\", zutaten=[");
            List<RecipeIngredientView> ings = ingredientsByRecipe.getOrDefault(c.recipe.id(), List.of());
            for (int i = 0; i < ings.size(); i++) {
                IngredientDto dto = names.get(ings.get(i).ingredientId());
                if (dto != null) {
                    if (i > 0) recipeList.append(", ");
                    recipeList.append(dto.name());
                }
            }
            recipeList.append("]\n");
        }

        return """
            Du hilfst bei der Rezeptauswahl. Alle unten gelisteten Rezepte sind \
            BEREITS gut durch den Vorrat abgedeckt. Verfügbarer Vorrat:
            %s
            Rezepte (verwende exakt die id aus der Liste):
            %s
            Gib für JEDES Rezept GENAU EINEN Eintrag mit kurzer positiver \
            Begründung (auf Deutsch) zurück, die die passenden VORHANDENEN \
            Zutaten benennt — erfinde KEINE fehlenden Zutaten. Sortiere nach \
            Passung zum Vorrat. Antworte AUSSCHLIESSLICH mit JSON im Format: \
            [{"recipeId":"<id>","reason":"<kurzer Grund>"}]. \
            Keine Erklärungen ausserhalb des JSON.
            """.formatted(pantryList, recipeList);
    }

    private Map<UUID, String> parseOllamaResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            // Drei Formen, die wir in echt gesehen haben:
            //   1) [{"recipeId":"...","reason":"..."}]   — der spec-conforme Fall
            //   2) {"recipeId":"...","reason":"..."}     — kleine Modelle geben oft nur ein Objekt
            //   3) {"suggestions":[...]} o.ae.            — wrap im Top-Level-Object
            List<JsonNode> entries;
            if (root.isArray()) {
                entries = toList((ArrayNode) root);
            } else if (root.isObject() && root.has("recipeId")) {
                entries = List.of(root);
            } else {
                JsonNode array = findFirstArray(root);
                entries = array != null && array.isArray()
                    ? toList((ArrayNode) array)
                    : List.of();
            }

            Map<UUID, String> result = new HashMap<>();
            for (JsonNode node : entries) {
                JsonNode idNode = node.get("recipeId");
                JsonNode reasonNode = node.get("reason");
                if (idNode == null || !idNode.isTextual()) {
                    continue;
                }
                try {
                    result.put(UUID.fromString(idNode.asText()),
                        reasonNode != null ? reasonNode.asText() : null);
                } catch (IllegalArgumentException ignore) {
                    // halluzinierte ID — übergehen
                }
            }
            return result;
        } catch (Exception ex) {
            LOG.warnf(ex, "Ollama-Antwort nicht parsbar: %s", body);
            return Map.of();
        }
    }

    private static List<JsonNode> toList(ArrayNode array) {
        List<JsonNode> out = new java.util.ArrayList<>(array.size());
        for (JsonNode n : array) {
            out.add(n);
        }
        return out;
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

    private record Candidate(RecipeDto recipe, double coverage) {
    }
}
