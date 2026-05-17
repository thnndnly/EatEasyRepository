package de.eateasy.suggestion.resource;

import de.eateasy.auth.repository.UserRepository;
import de.eateasy.common.units.Unit;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.pantry.repository.PantryItemRepository;
import de.eateasy.recipe.repository.RecipeRepository;
import de.eateasy.suggestion.client.OllamaClient;
import de.eateasy.suggestion.client.OllamaGenerateRequest;
import de.eateasy.suggestion.client.OllamaGenerateResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

@QuarkusTest
class HouseholdSuggestionResourceTest {

    @InjectMock
    @RestClient
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
    @DisplayName("POST /households/{id}/suggestions liefert Vorschlaege")
    void happyPath() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "WG");
        addPantry(token, householdId, "Tomate");
        addPantry(token, householdId, "Basilikum");
        String recipeId = createRecipe(token, householdId, "Tomatensalat",
            List.of("Tomate", "Basilikum"));

        String ollamaJson = "[{\"recipeId\":\"" + recipeId + "\","
            + "\"reason\":\"Alles da\"}]";
        when(ollamaClient.generate(ArgumentMatchers.any(OllamaGenerateRequest.class)))
            .thenReturn(new OllamaGenerateResponse("llama3", ollamaJson, true));

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("numSuggestions", 3))
            .when().post("/api/v1/households/" + householdId + "/suggestions")
            .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].recipe.title", equalTo("Tomatensalat"))
                .body("[0].reason", equalTo("Alles da"));
    }

    @Test
    @DisplayName("POST /households/{id}/suggestions ohne Vorrat → leere Liste")
    void emptyPantry() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "WG");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("numSuggestions", 3))
            .when().post("/api/v1/households/" + householdId + "/suggestions")
            .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("POST fuer fremden Haushalt → 403")
    void foreignHousehold() {
        String alice = registerUser("alice@example.com");
        String bob = registerUser("bob@example.com");
        String aliceHouse = createHousehold(alice, "Alice");

        given()
            .header("Authorization", "Bearer " + bob)
            .contentType(ContentType.JSON)
            .body(Map.of("numSuggestions", 3))
            .when().post("/api/v1/households/" + aliceHouse + "/suggestions")
            .then().statusCode(403);
    }

    @Test
    @DisplayName("POST mit numSuggestions=0 → 400 (Validation)")
    void invalidNumSuggestions() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "WG");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("numSuggestions", 0))
            .when().post("/api/v1/households/" + householdId + "/suggestions")
            .then().statusCode(400);
    }

    // --- Helpers ---------------------------------------------------------

    private static String registerUser(String email) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", "secret12", "displayName", email))
            .when().post("/api/v1/auth/register")
            .then().statusCode(201).extract().path("token");
    }

    private static String createHousehold(String token, String name) {
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
            .when().post("/api/v1/households")
            .then().statusCode(201).extract().path("id");
    }

    private static void addPantry(String token, String householdId, String ingredientName) {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "ingredientName", ingredientName,
                "amount", 1,
                "unit", Unit.PIECE.name()))
            .when().post("/api/v1/households/" + householdId + "/pantry")
            .then().statusCode(201);
    }

    private static String createRecipe(String token, String householdId, String title,
                                       List<String> ingredientNames) {
        List<Map<String, Object>> ings = ingredientNames.stream()
            .map(n -> Map.<String, Object>of(
                "ingredientName", n,
                "amount", 1,
                "unit", Unit.PIECE.name()))
            .toList();
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", title,
                "instructions", "Steps",
                "servings", 2,
                "householdId", householdId,
                "ingredients", ings))
            .when().post("/api/v1/recipes")
            .then().statusCode(201).extract().path("id");
    }
}
