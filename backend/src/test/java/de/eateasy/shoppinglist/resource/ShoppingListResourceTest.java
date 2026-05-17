package de.eateasy.shoppinglist.resource;

import de.eateasy.auth.repository.UserRepository;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.mealplan.repository.MealPlanEntryRepository;
import de.eateasy.mealplan.repository.MealPlanRepository;
import de.eateasy.pantry.repository.PantryItemRepository;
import de.eateasy.recipe.repository.RecipeRepository;
import de.eateasy.shoppinglist.repository.ShoppingListItemRepository;
import de.eateasy.shoppinglist.repository.ShoppingListRepository;
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
import static org.hamcrest.Matchers.is;

@QuarkusTest
class ShoppingListResourceTest {

    @Inject
    ShoppingListItemRepository itemRepository;

    @Inject
    ShoppingListRepository listRepository;

    @Inject
    PantryItemRepository pantryRepository;

    @Inject
    MealPlanEntryRepository mealPlanEntryRepository;

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
        itemRepository.deleteAll();
        listRepository.deleteAll();
        pantryRepository.deleteAll();
        mealPlanEntryRepository.deleteAll();
        mealPlanRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /shoppinglist ohne Token liefert 401")
    void unauthenticated() {
        given()
            .when().get("/api/v1/mealplans/" + java.util.UUID.randomUUID() + "/shoppinglist")
            .then().statusCode(401);
    }

    @Test
    @DisplayName("GET /shoppinglist generiert Liste lazy")
    void getOrGenerate() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Test");
        String recipeId = createRecipe(token, householdId, "Suppe");
        String planId = getMealPlan(token, householdId);
        setEntry(token, planId, recipeId);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/v1/mealplans/" + planId + "/shoppinglist")
            .then()
                .statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].ingredientName", equalTo("Salz"))
                .body("items[0].checked", is(false));
    }

    @Test
    @DisplayName("PATCH /shoppinglist/items/{id} toggelt checked")
    void toggleChecked() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Test");
        String recipeId = createRecipe(token, householdId, "Suppe");
        String planId = getMealPlan(token, householdId);
        setEntry(token, planId, recipeId);
        String itemId = given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/v1/mealplans/" + planId + "/shoppinglist")
            .then().statusCode(200)
            .extract().path("items[0].id");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("checked", true))
            .when().patch("/api/v1/shoppinglist/items/" + itemId)
            .then()
                .statusCode(200)
                .body("checked", is(true));
    }

    @Test
    @DisplayName("POST /shoppinglist/regenerate: gechecktes Item liegt jetzt im Vorrat → faellt aus Liste raus")
    void regenerateAfterCheckedDropsItem() {
        // Mit Auto-Nachbuchen (Phase 10/Post-MVP): toggleChecked(true) bucht
        // den Posten in den Vorrat. Beim Regenerate zieht subtractPantry den
        // Eintrag wieder ab — Item ist weg.
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Test");
        String recipeId = createRecipe(token, householdId, "Suppe");
        String planId = getMealPlan(token, householdId);
        setEntry(token, planId, recipeId);

        String itemId = given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/v1/mealplans/" + planId + "/shoppinglist")
            .then().statusCode(200).extract().path("items[0].id");
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("checked", true))
            .when().patch("/api/v1/shoppinglist/items/" + itemId)
            .then().statusCode(200);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .when().post("/api/v1/mealplans/" + planId + "/shoppinglist/regenerate")
            .then()
                .statusCode(200)
                .body("items.size()", is(0));
    }

    @Test
    @DisplayName("GET /shoppinglist fuer fremden Plan liefert 403")
    void getForbiddenForOutsider() {
        String aliceToken = registerUser("alice@example.com");
        String bobToken = registerUser("bob@example.com");
        String householdId = createHousehold(aliceToken, "Alice");
        String planId = getMealPlan(aliceToken, householdId);

        given()
            .header("Authorization", "Bearer " + bobToken)
            .when().get("/api/v1/mealplans/" + planId + "/shoppinglist")
            .then().statusCode(403);
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

    private static String createRecipe(String token, String householdId, String title) {
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", title,
                "instructions", "Steps",
                "servings", 2,
                "householdId", householdId,
                "ingredients", List.of(Map.of(
                    "ingredientName", "Salz", "amount", 5, "unit", "GRAM"))))
            .when().post("/api/v1/recipes")
            .then().statusCode(201).extract().path("id");
    }

    private static String getMealPlan(String token, String householdId) {
        return given()
            .header("Authorization", "Bearer " + token)
            .queryParam("weekStart", "2026-04-27")
            .when().get("/api/v1/households/" + householdId + "/mealplans")
            .then().statusCode(200).extract().path("id");
    }

    private static void setEntry(String token, String planId, String recipeId) {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "dayOfWeek", "MONDAY",
                "mealType", "LUNCH",
                "recipeId", recipeId,
                "servings", 2))
            .when().put("/api/v1/mealplans/" + planId + "/entries")
            .then().statusCode(200);
    }
}
