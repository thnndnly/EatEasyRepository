package de.eateasy.integration.resource;

import de.eateasy.auth.repository.UserRepository;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.integration.client.TheMealDbClient;
import de.eateasy.integration.client.TheMealDbResponse;
import de.eateasy.integration.client.TheMealDbResponse.TheMealDbMeal;
import de.eateasy.recipe.repository.RecipeRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class RecipeImportResourceTest {

    @InjectMock
    @RestClient
    TheMealDbClient theMealDbClient;

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
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /integration/recipes/search liefert Previews")
    void searchEndpoint() {
        String token = registerUser("alice@example.com");
        when(theMealDbClient.search(any())).thenReturn(
            new TheMealDbResponse(List.of(meal("1", "Spaghetti"))));

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("source", "themealdb")
            .queryParam("q", "pasta")
            .when().get("/api/v1/integration/recipes/search")
            .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Spaghetti"));
    }

    @Test
    @DisplayName("POST /recipes/import erstellt Recipe aus TheMealDB")
    void importEndpoint() {
        String token = registerUser("alice@example.com");
        when(theMealDbClient.lookup("52772")).thenReturn(
            new TheMealDbResponse(List.of(meal("52772", "Teriyaki"))));

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("source", "themealdb", "externalId", "52772"))
            .when().post("/api/v1/recipes/import")
            .then()
                .statusCode(201)
                .body("title", equalTo("Teriyaki"))
                .body("externalSource", equalTo("themealdb"));
    }

    @Test
    @DisplayName("POST /recipes/import liefert 404 bei unbekannter externalId")
    void importNotFound() {
        String token = registerUser("alice@example.com");
        when(theMealDbClient.lookup(any())).thenReturn(new TheMealDbResponse(null));

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("source", "themealdb", "externalId", "999"))
            .when().post("/api/v1/recipes/import")
            .then().statusCode(404);
    }

    @Test
    @DisplayName("POST /recipes/import lehnt unbekannte Quelle ab")
    void importRejectsUnknownSource() {
        String token = registerUser("alice@example.com");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("source", "unknown", "externalId", "1"))
            .when().post("/api/v1/recipes/import")
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

    private static TheMealDbMeal meal(String id, String title) {
        return new TheMealDbMeal(
            id, title, "Other", "Local", "Steps.", null, "https://example.com/" + id,
            "salt", null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            "1g", null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null);
    }
}
