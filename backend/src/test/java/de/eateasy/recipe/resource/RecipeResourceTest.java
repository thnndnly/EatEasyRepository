package de.eateasy.recipe.resource;

import de.eateasy.auth.repository.UserRepository;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.recipe.repository.RecipeRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class RecipeResourceTest {

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
    @DisplayName("POST /recipes ohne Token liefert 401")
    void createUnauthenticated() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("title", "Test"))
            .when().post("/api/v1/recipes")
            .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST /recipes legt Rezept mit Zutaten an")
    void createRecipe() {
        String token = registerUser("alice@example.com", "Alice");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", "Tomatensuppe",
                "instructions", "Tomaten kochen",
                "servings", 4,
                "ingredients", List.of(Map.of(
                    "ingredientName", "Tomate",
                    "amount", 500,
                    "unit", "GRAM"))))
            .when().post("/api/v1/recipes")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Tomatensuppe"))
                .body("servings", equalTo(4))
                .body("ingredients[0].ingredientName", equalTo("Tomate"));
    }

    @Test
    @DisplayName("POST /recipes mit servings <= 0 liefert 400")
    void createRecipeRejectsBadServings() {
        String token = registerUser("alice@example.com", "Alice");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", "Test",
                "instructions", "Steps",
                "servings", 0,
                "ingredients", List.of(Map.of(
                    "ingredientName", "Salz", "amount", 1, "unit", "GRAM"))))
            .when().post("/api/v1/recipes")
            .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /recipes liefert nur sichtbare Rezepte")
    void listFiltersVisibility() {
        String aliceToken = registerUser("alice@example.com", "Alice");
        String bobToken = registerUser("bob@example.com", "Bob");

        createRecipe(aliceToken, "Alice Recipe");
        createRecipe(bobToken, "Bob Recipe");

        given()
            .header("Authorization", "Bearer " + aliceToken)
            .when().get("/api/v1/recipes")
            .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Alice Recipe"));
    }

    @Test
    @DisplayName("GET /recipes?q= sucht im Titel")
    void listFilterByQuery() {
        String token = registerUser("alice@example.com", "Alice");
        createRecipe(token, "Tomatensuppe");
        createRecipe(token, "Pizza Margherita");

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("q", "Tomate")
            .when().get("/api/v1/recipes")
            .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Tomatensuppe"));
    }

    @Test
    @DisplayName("GET /recipes?dietTags=vegan filtert auf Tag")
    void listFilterByDietTag() {
        String token = registerUser("alice@example.com", "Alice");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", "Vegan", "instructions", "x", "servings", 2,
                "dietTags", List.of("vegan"),
                "ingredients", List.of(Map.of("ingredientName", "Salz", "amount", 1, "unit", "GRAM"))))
            .when().post("/api/v1/recipes")
            .then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", "Plain", "instructions", "x", "servings", 2,
                "ingredients", List.of(Map.of("ingredientName", "Salz", "amount", 1, "unit", "GRAM"))))
            .when().post("/api/v1/recipes")
            .then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("dietTags", "vegan")
            .when().get("/api/v1/recipes")
            .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Vegan"));
    }

    @Test
    @DisplayName("GET /recipes/{id} fuer fremdes Rezept liefert 403")
    void getForbiddenForOutsider() {
        String aliceToken = registerUser("alice@example.com", "Alice");
        String bobToken = registerUser("bob@example.com", "Bob");
        String aliceRecipeId = createRecipe(aliceToken, "Alice Privat");

        given()
            .header("Authorization", "Bearer " + bobToken)
            .when().get("/api/v1/recipes/" + aliceRecipeId)
            .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("DELETE /recipes/{id} als Nicht-Owner liefert 403")
    void deleteAsNonOwner() {
        String aliceToken = registerUser("alice@example.com", "Alice");
        String bobToken = registerUser("bob@example.com", "Bob");
        String recipeId = createRecipe(aliceToken, "Alice Recipe");

        given()
            .header("Authorization", "Bearer " + bobToken)
            .when().delete("/api/v1/recipes/" + recipeId)
            .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("PATCH /recipes/{id} als Owner aendert das Rezept")
    void patchAsOwner() {
        String token = registerUser("alice@example.com", "Alice");
        String recipeId = createRecipe(token, "Original");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", "Geaendert",
                "instructions", "Neue Steps",
                "servings", 4,
                "ingredients", List.of(Map.of("ingredientName", "Mehl", "amount", 200, "unit", "GRAM"))))
            .when().patch("/api/v1/recipes/" + recipeId)
            .then()
                .statusCode(200)
                .body("title", equalTo("Geaendert"))
                .body("servings", equalTo(4))
                .body("ingredients[0].ingredientName", equalTo("Mehl"));
    }

    // --- Helpers ---------------------------------------------------------

    private static String registerUser(String email, String displayName) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", "secret12", "displayName", displayName))
            .when().post("/api/v1/auth/register")
            .then().statusCode(201)
            .extract().path("token");
    }

    private static String createRecipe(String token, String title) {
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", title,
                "instructions", "Steps",
                "servings", 2,
                "ingredients", List.of(Map.of(
                    "ingredientName", "Salz", "amount", 1, "unit", "GRAM"))))
            .when().post("/api/v1/recipes")
            .then().statusCode(201)
            .extract().path("id");
    }
}
