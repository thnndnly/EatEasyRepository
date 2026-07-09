package de.eateasy.suggestion.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.repository.UserRepository;
import de.eateasy.auth.service.AuthService;
import de.eateasy.common.exception.ForbiddenException;
import de.eateasy.common.units.Unit;
import de.eateasy.household.dto.HouseholdCreateRequest;
import de.eateasy.household.dto.HouseholdDto;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.household.service.HouseholdService;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.pantry.dto.AddPantryItemRequest;
import de.eateasy.pantry.repository.PantryItemRepository;
import de.eateasy.pantry.service.PantryService;
import de.eateasy.recipe.dto.RecipeCreateRequest;
import de.eateasy.recipe.dto.RecipeDto;
import de.eateasy.recipe.dto.RecipeIngredientRequest;
import de.eateasy.recipe.repository.RecipeRepository;
import de.eateasy.recipe.service.RecipeService;
import de.eateasy.suggestion.client.OllamaClient;
import de.eateasy.suggestion.client.OllamaGenerateRequest;
import de.eateasy.suggestion.client.OllamaGenerateResponse;
import de.eateasy.suggestion.dto.SuggestionDto;
import de.eateasy.suggestion.dto.SuggestionResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@QuarkusTest
class SmartSuggestionServiceImplTest {

    @Inject
    SmartSuggestionService suggestionService;

    @Inject
    AuthService authService;

    @Inject
    HouseholdService householdService;

    @Inject
    PantryService pantryService;

    @Inject
    RecipeService recipeService;

    @InjectMock
    OllamaClient ollamaClient;

    @Inject
    PantryItemRepository pantryRepository;

    @Inject
    RecipeRepository recipeRepository;

    @Inject
    IngredientRepository ingredientRepository;

    @Inject
    HouseholdInvitationRepository invitationRepository;

    @Inject
    HouseholdMembershipRepository membershipRepository;

    @Inject
    HouseholdRepository householdRepository;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    @Transactional
    void cleanUp() {
        pantryRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Liefert Vorschläge mit Begründung aus Ollama-Antwort")
    void happyPath() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = createHousehold(userId, "WG").id();
        addPantry(userId, householdId, "Tomate", Unit.PIECE);
        addPantry(userId, householdId, "Basilikum", Unit.PIECE);
        RecipeDto recipe = createRecipe(userId, householdId, "Tomatensalat",
            List.of(("Tomate"), ("Basilikum")));

        String ollamaJson = "[{\"recipeId\":\"" + recipe.id() + "\","
            + "\"reason\":\"Alle Zutaten im Vorrat\"}]";
        when(ollamaClient.generate(ArgumentMatchers.any(OllamaGenerateRequest.class)))
            .thenReturn(new OllamaGenerateResponse("llama3", ollamaJson, true));

        SuggestionResponse response = suggestionService.suggest(userId, householdId, 3);
        List<SuggestionDto> suggestions = response.suggestions();

        assertThat(response.aiAvailable()).isTrue();
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).recipe().title()).isEqualTo("Tomatensalat");
        assertThat(suggestions.get(0).reason()).isEqualTo("Alle Zutaten im Vorrat");
        assertThat(suggestions.get(0).coverage()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Single-Object-Antwort wird ebenfalls akzeptiert (kleine Modelle)")
    void singleObjectResponse() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = createHousehold(userId, "WG").id();
        addPantry(userId, householdId, "Tomate", Unit.PIECE);
        RecipeDto recipe = createRecipe(userId, householdId, "Salat",
            List.of("Tomate"));

        // Kein Array — direkt das einzelne Objekt. Beobachtet bei llama3.2.
        String ollamaJson = "{\"recipeId\":\"" + recipe.id() + "\","
            + "\"reason\":\"Tomate verfügbar\"}";
        when(ollamaClient.generate(ArgumentMatchers.any(OllamaGenerateRequest.class)))
            .thenReturn(new OllamaGenerateResponse("llama3", ollamaJson, true));

        SuggestionResponse response = suggestionService.suggest(userId, householdId, 3);
        List<SuggestionDto> suggestions = response.suggestions();

        assertThat(response.aiAvailable()).isTrue();
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).reason()).isEqualTo("Tomate verfügbar");
    }

    @Test
    @DisplayName("Ollama-Fehler → Fallback ohne reason, aber mit Coverage-Reihenfolge")
    void ollamaFailureFallsBack() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = createHousehold(userId, "WG").id();
        addPantry(userId, householdId, "Tomate", Unit.PIECE);
        addPantry(userId, householdId, "Basilikum", Unit.PIECE);
        RecipeDto recipe = createRecipe(userId, householdId, "Tomatensalat",
            List.of("Tomate", "Basilikum"));

        when(ollamaClient.generate(ArgumentMatchers.any(OllamaGenerateRequest.class)))
            .thenThrow(new RuntimeException("Ollama down"));

        SuggestionResponse response = suggestionService.suggest(userId, householdId, 3);
        List<SuggestionDto> suggestions = response.suggestions();

        // Ollama-Fehler → aiAvailable=false signalisiert die stille Degradierung.
        assertThat(response.aiAvailable()).isFalse();
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).recipe().title()).isEqualTo("Tomatensalat");
        assertThat(suggestions.get(0).reason()).isNull();
        assertThat(suggestions.get(0).coverage()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Unparsbare Ollama-Antwort → Fallback")
    void unparsableJsonFallsBack() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = createHousehold(userId, "WG").id();
        addPantry(userId, householdId, "Tomate", Unit.PIECE);
        RecipeDto recipe = createRecipe(userId, householdId, "Pasta",
            List.of("Tomate"));

        when(ollamaClient.generate(ArgumentMatchers.any(OllamaGenerateRequest.class)))
            .thenReturn(new OllamaGenerateResponse("llama3", "ich bin kein json", true));

        SuggestionResponse response = suggestionService.suggest(userId, householdId, 3);
        List<SuggestionDto> suggestions = response.suggestions();

        assertThat(response.aiAvailable()).isFalse();
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).reason()).isNull();
    }

    @Test
    @DisplayName("Rezept unter Coverage-Schwelle wird gefiltert")
    void filtersBelowThreshold() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = createHousehold(userId, "WG").id();
        addPantry(userId, householdId, "Tomate", Unit.PIECE);
        // 4 Zutaten, nur 1 im Vorrat → coverage = 0.25 < 0.5
        createRecipe(userId, householdId, "Komplex",
            List.of("Tomate", "Reis", "Fisch", "Curry"));

        // Ollama wird nicht aufgerufen, weil keine Kandidaten → kein Mock-Setup nötig.

        SuggestionResponse response = suggestionService.suggest(userId, householdId, 3);
        List<SuggestionDto> suggestions = response.suggestions();

        assertThat(suggestions).isEmpty();
    }

    @Test
    @DisplayName("Leerer Vorrat → leere Vorschlagsliste, kein Ollama-Call")
    void emptyPantryReturnsEmpty() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = createHousehold(userId, "WG").id();
        createRecipe(userId, householdId, "Foo", List.of("Tomate"));

        SuggestionResponse response = suggestionService.suggest(userId, householdId, 3);
        List<SuggestionDto> suggestions = response.suggestions();

        assertThat(suggestions).isEmpty();
    }

    @Test
    @DisplayName("Nicht-Mitglied bekommt 403")
    void nonMemberForbidden() {
        UUID alice = registerUser("alice@example.com");
        UUID bob = registerUser("bob@example.com");
        UUID aliceHouse = createHousehold(alice, "Alice").id();

        assertThatThrownBy(() -> suggestionService.suggest(bob, aliceHouse, 3))
            .isInstanceOf(ForbiddenException.class);
    }

    // --- Helpers ---------------------------------------------------------

    private UUID registerUser(String email) {
        AuthResponse response = authService.register(
            new RegisterRequest(email, "secret12", email));
        return response.user().id();
    }

    private HouseholdDto createHousehold(UUID userId, String name) {
        return householdService.create(userId, new HouseholdCreateRequest(name, null));
    }

    private void addPantry(UUID userId, UUID householdId, String ingredientName, Unit unit) {
        pantryService.add(userId, householdId, new AddPantryItemRequest(
            null, ingredientName, BigDecimal.ONE, unit, null));
    }

    private RecipeDto createRecipe(UUID userId, UUID householdId, String title,
                                   List<String> ingredientNames) {
        List<RecipeIngredientRequest> ings = ingredientNames.stream()
            .map(n -> new RecipeIngredientRequest(
                null, n, BigDecimal.ONE, Unit.PIECE, null))
            .toList();
        return recipeService.create(userId, new RecipeCreateRequest(
            title, "desc", "Steps", 2, null, List.of(), householdId, ings));
    }
}
