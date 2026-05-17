package de.eateasy.integration.resource;

import de.eateasy.auth.repository.UserRepository;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.integration.client.OpenFoodFactsClient;
import de.eateasy.integration.client.OpenFoodFactsResponse;
import de.eateasy.integration.client.OpenFoodFactsResponse.Product;
import de.eateasy.pantry.repository.PantryItemRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class BarcodeResourceTest {

    @InjectMock
    @RestClient
    OpenFoodFactsClient openFoodFactsClient;

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
    @DisplayName("GET /integration/products/{barcode} liefert Preview")
    void lookupEndpoint() {
        String token = registerUser("alice@example.com");
        when(openFoodFactsClient.getProduct("3017620422003")).thenReturn(
            new OpenFoodFactsResponse("3017620422003", 1,
                new Product("Nutella", "Nuss-Nougat-Creme", "400 g")));

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/v1/integration/products/3017620422003")
            .then()
                .statusCode(200)
                .body("name", equalTo("Nuss-Nougat-Creme"))
                .body("suggestedUnit", equalTo("GRAM"));
    }

    @Test
    @DisplayName("GET /integration/products/{barcode} liefert 404 bei unbekanntem Barcode")
    void lookupNotFound() {
        String token = registerUser("alice@example.com");
        when(openFoodFactsClient.getProduct(any())).thenReturn(
            new OpenFoodFactsResponse("000", 0, null));

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/v1/integration/products/000")
            .then().statusCode(404);
    }

    @Test
    @DisplayName("POST /households/{id}/pantry/barcode legt PantryItem an")
    void addByBarcode() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Alice");
        when(openFoodFactsClient.getProduct("555")).thenReturn(
            new OpenFoodFactsResponse("555", 1,
                new Product("Olive Oil", "Olivenoel", "500 ml")));

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "barcode", "555",
                "amount", 500,
                "unit", "ML"))
            .when().post("/api/v1/households/" + householdId + "/pantry/barcode")
            .then()
                .statusCode(201)
                .body("ingredientName", equalTo("Olivenoel"))
                .body("unit", equalTo("ML"));
    }

    @Test
    @DisplayName("POST /households/{id}/pantry/barcode mit unbekanntem Barcode liefert 404")
    void addByBarcodeNotFound() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token, "Alice");
        when(openFoodFactsClient.getProduct(any())).thenReturn(
            new OpenFoodFactsResponse("000", 0, null));

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "barcode", "000",
                "amount", 1,
                "unit", "PIECE"))
            .when().post("/api/v1/households/" + householdId + "/pantry/barcode")
            .then().statusCode(404);
    }

    @Test
    @DisplayName("POST /households/{id}/pantry/barcode fuer fremden Haushalt liefert 403")
    void addByBarcodeForeignHousehold() {
        String aliceToken = registerUser("alice@example.com");
        String bobToken = registerUser("bob@example.com");
        String aliceHouseId = createHousehold(aliceToken, "Alice");
        when(openFoodFactsClient.getProduct("555")).thenReturn(
            new OpenFoodFactsResponse("555", 1,
                new Product("Foo", "Foo", "100 g")));

        given()
            .header("Authorization", "Bearer " + bobToken)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "barcode", "555",
                "amount", 100,
                "unit", "GRAM"))
            .when().post("/api/v1/households/" + aliceHouseId + "/pantry/barcode")
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
}
