package de.eateasy.mealplan.resource;

import de.eateasy.auth.repository.UserRepository;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.mealplan.repository.MealPlanEntryRepository;
import de.eateasy.mealplan.repository.MealPlanRepository;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class MealPlanResourceTest {

    @Inject
    MealPlanEntryRepository entryRepository;

    @Inject
    MealPlanRepository mealPlanRepository;

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
        entryRepository.deleteAll();
        mealPlanRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /households/{id}/mealplans ohne Token liefert 401")
    void unauthenticated() {
        given()
            .when().get("/api/v1/households/" + java.util.UUID.randomUUID() + "/mealplans")
            .then().statusCode(401);
    }

    @Test
    @DisplayName("GET /households/{id}/mealplans?weekStart=... legt Plan an und liefert ihn")
    void getOrCreatePlan() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Test");

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("weekStart", "2026-04-29")
            .when().get("/api/v1/households/" + householdId + "/mealplans")
            .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("weekStart", equalTo("2026-04-27"))
                .body("entries", hasSize(0));
    }

    @Test
    @DisplayName("GET /households/{id}/mealplans fuer fremden Haushalt liefert 403")
    void getMealPlanForeignHousehold() {
        String aliceToken = registerUser("alice@example.com");
        String bobToken = registerUser("bob@example.com");
        String aliceHouseId = createHousehold(aliceToken, "Alice");

        given()
            .header("Authorization", "Bearer " + bobToken)
            .when().get("/api/v1/households/" + aliceHouseId + "/mealplans")
            .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("PUT /mealplans/{id}/entries setzt einen Slot")
    void putSetEntry() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Test");
        String recipeId = createRecipe(token, householdId, "Suppe");
        String planId = getMealPlan(token, householdId);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "dayOfWeek", "MONDAY",
                "mealType", "LUNCH",
                "recipeId", recipeId,
                "servings", 4))
            .when().put("/api/v1/mealplans/" + planId + "/entries")
            .then()
                .statusCode(200)
                .body("dayOfWeek", equalTo("MONDAY"))
                .body("mealType", equalTo("LUNCH"))
                .body("servings", equalTo(4))
                .body("recipe.title", equalTo("Suppe"));
    }

    @Test
    @DisplayName("PUT /mealplans/{id}/entries ueberschreibt vorhandenen Slot")
    void putOverwritesEntry() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Test");
        String r1 = createRecipe(token, householdId, "Erstes");
        String r2 = createRecipe(token, householdId, "Zweites");
        String planId = getMealPlan(token, householdId);

        setEntry(token, planId, "MONDAY", "LUNCH", r1, 2);
        setEntry(token, planId, "MONDAY", "LUNCH", r2, 6);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("weekStart", "2026-04-27")
            .when().get("/api/v1/households/" + householdId + "/mealplans")
            .then()
                .statusCode(200)
                .body("entries", hasSize(1))
                .body("entries[0].recipe.title", equalTo("Zweites"))
                .body("entries[0].servings", equalTo(6));
    }

    @Test
    @DisplayName("DELETE /mealplans/{id}/entries/{day}/{mealType} loescht den Slot")
    void deleteEntry() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Test");
        String recipeId = createRecipe(token, householdId, "Suppe");
        String planId = getMealPlan(token, householdId);
        setEntry(token, planId, "WEDNESDAY", "DINNER", recipeId, 2);

        given()
            .header("Authorization", "Bearer " + token)
            .when().delete("/api/v1/mealplans/" + planId + "/entries/WEDNESDAY/DINNER")
            .then().statusCode(204);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("weekStart", "2026-04-27")
            .when().get("/api/v1/households/" + householdId + "/mealplans")
            .then()
                .statusCode(200)
                .body("entries", hasSize(0));
    }

    @Test
    @DisplayName("PUT /mealplans/{id}/entries fuer fremden Plan liefert 403")
    void putEntryForbiddenForOutsider() {
        String aliceToken = registerUser("alice@example.com");
        String bobToken = registerUser("bob@example.com");
        String aliceHouseId = createHousehold(aliceToken, "Alice");
        String aliceRecipeId = createRecipe(aliceToken, aliceHouseId, "Suppe");
        String alicePlanId = getMealPlan(aliceToken, aliceHouseId);

        given()
            .header("Authorization", "Bearer " + bobToken)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "dayOfWeek", "MONDAY",
                "mealType", "LUNCH",
                "recipeId", aliceRecipeId,
                "servings", 2))
            .when().put("/api/v1/mealplans/" + alicePlanId + "/entries")
            .then()
                .statusCode(403);
    }

    // --- Helpers ---------------------------------------------------------

    private static String registerUser(String email) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", "secret12", "displayName", email))
            .when().post("/api/v1/auth/register")
            .then().statusCode(201)
            .extract().path("token");
    }

    private static String createHousehold(String token, String name) {
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
            .when().post("/api/v1/households")
            .then().statusCode(201)
            .extract().path("id");
    }

    private static String createRecipe(String token, String householdId, String title) {
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", title,
                "instructions", "Steps",
                "servings", 2,
                "householdId", householdId,
                "ingredients", List.of(Map.of("ingredientName", "Salz", "amount", 1, "unit", "GRAM"))))
            .when().post("/api/v1/recipes")
            .then().statusCode(201)
            .extract().path("id");
    }

    private static String getMealPlan(String token, String householdId) {
        return given()
            .header("Authorization", "Bearer " + token)
            .queryParam("weekStart", "2026-04-27")
            .when().get("/api/v1/households/" + householdId + "/mealplans")
            .then().statusCode(200)
            .extract().path("id");
    }

    private static void setEntry(String token, String planId, String day, String mealType,
                                  String recipeId, int servings) {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "dayOfWeek", day,
                "mealType", mealType,
                "recipeId", recipeId,
                "servings", servings))
            .when().put("/api/v1/mealplans/" + planId + "/entries")
            .then().statusCode(200);
    }
}
