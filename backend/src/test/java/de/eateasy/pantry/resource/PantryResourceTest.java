package de.eateasy.pantry.resource;

import de.eateasy.auth.repository.UserRepository;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.pantry.repository.PantryItemRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class PantryResourceTest {

    @Inject
    PantryItemRepository pantryRepository;

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
        ingredientRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /households/{id}/pantry ohne Token liefert 401")
    void unauthenticated() {
        given()
            .when().get("/api/v1/households/" + java.util.UUID.randomUUID() + "/pantry")
            .then().statusCode(401);
    }

    @Test
    @DisplayName("POST /households/{id}/pantry legt Eintrag an")
    void addItem() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Test");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "ingredientName", "Tomate",
                "amount", 500,
                "unit", "GRAM",
                "bestBefore", "2026-06-01"))
            .when().post("/api/v1/households/" + householdId + "/pantry")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("ingredientName", equalTo("Tomate"))
                .body("amount", equalTo(500.0F))
                .body("unit", equalTo("GRAM"))
                .body("bestBefore", equalTo("2026-06-01"));
    }

    @Test
    @DisplayName("POST aggregiert Menge bei gleicher Zutat + Unit")
    void addAggregates() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Test");
        addItem(token, householdId, "Tomate", 500, "GRAM");
        addItem(token, householdId, "Tomate", 250, "GRAM");

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/v1/households/" + householdId + "/pantry")
            .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].amount", equalTo(750.0F));
    }

    @Test
    @DisplayName("GET für fremden Haushalt liefert 403")
    void listForbiddenForOutsider() {
        String aliceToken = registerUser("alice@example.com");
        String bobToken = registerUser("bob@example.com");
        String aliceHouse = createHousehold(aliceToken, "Alice");

        given()
            .header("Authorization", "Bearer " + bobToken)
            .when().get("/api/v1/households/" + aliceHouse + "/pantry")
            .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("PATCH /pantry/{id} ändert Menge")
    void patchItem() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Test");
        String itemId = addItem(token, householdId, "Tomate", 500, "GRAM");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("amount", 200))
            .when().patch("/api/v1/pantry/" + itemId)
            .then()
                .statusCode(200)
                .body("amount", equalTo(200.0F));
    }

    @Test
    @DisplayName("DELETE /pantry/{id} entfernt den Eintrag")
    void deleteItem() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Test");
        String itemId = addItem(token, householdId, "Tomate", 500, "GRAM");

        given()
            .header("Authorization", "Bearer " + token)
            .when().delete("/api/v1/pantry/" + itemId)
            .then().statusCode(204);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/v1/households/" + householdId + "/pantry")
            .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("PATCH /pantry/{id} für fremden Eintrag liefert 403")
    void patchForbiddenForOutsider() {
        String aliceToken = registerUser("alice@example.com");
        String bobToken = registerUser("bob@example.com");
        String aliceHouse = createHousehold(aliceToken, "Alice");
        String aliceItemId = addItem(aliceToken, aliceHouse, "Salz", 100, "GRAM");

        given()
            .header("Authorization", "Bearer " + bobToken)
            .contentType(ContentType.JSON)
            .body(Map.of("amount", 999))
            .when().patch("/api/v1/pantry/" + aliceItemId)
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

    private static String addItem(String token, String householdId, String name,
                                   int amount, String unit) {
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "ingredientName", name,
                "amount", amount,
                "unit", unit))
            .when().post("/api/v1/households/" + householdId + "/pantry")
            .then().statusCode(201)
            .extract().path("id");
    }
}
