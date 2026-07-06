package de.eateasy.receipt.resource;

import de.eateasy.auth.repository.UserRepository;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.receipt.client.OcrClient;
import de.eateasy.suggestion.client.OllamaClient;
import de.eateasy.suggestion.client.OllamaGenerateResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class ReceiptResourceTest {

    @InjectMock
    OcrClient ocrClient;

    @InjectMock
    OllamaClient ollamaClient;

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
        ingredientRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /receipts/scan ohne Token liefert 401")
    void scanUnauthenticated() {
        given()
            .multiPart("file", "bon.jpg", new byte[] {1, 2, 3}, "image/jpeg")
            .when().post("/api/v1/households/" + UUID.randomUUID() + "/receipts/scan")
            .then().statusCode(401);
    }

    @Test
    @DisplayName("POST /receipts/scan liefert Rohtext + Items (Clients gemockt)")
    void scanHappyPath() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token);

        when(ocrClient.extractText(any(), anyString())).thenReturn("REWE Milch 1,19");
        when(ollamaClient.generate(any())).thenReturn(new OllamaGenerateResponse(
            "llama3",
            "[{\"name\":\"Milch\",\"amount\":1000,\"unit\":\"ML\"}]",
            true));

        given()
            .header("Authorization", "Bearer " + token)
            .multiPart("file", "bon.jpg", new byte[] {1, 2, 3}, "image/jpeg")
            .when().post("/api/v1/households/" + householdId + "/receipts/scan")
            .then()
                .statusCode(200)
                .body("rawText", notNullValue())
                .body("items", hasSize(1))
                .body("items[0].name", equalTo("Milch"))
                .body("items[0].unit", equalTo("ML"));
    }

    @Test
    @DisplayName("POST /receipts/scan fuer fremden Haushalt liefert 403")
    void scanForbiddenForOutsider() {
        String aliceToken = registerUser("alice@example.com");
        String bobToken = registerUser("bob@example.com");
        String householdId = createHousehold(aliceToken);

        given()
            .header("Authorization", "Bearer " + bobToken)
            .multiPart("file", "bon.jpg", new byte[] {1, 2, 3}, "image/jpeg")
            .when().post("/api/v1/households/" + householdId + "/receipts/scan")
            .then().statusCode(403);
    }

    @Test
    @DisplayName("POST /receipts/scan ohne Datei liefert 400")
    void scanWithoutFileRejected() {
        String token = registerUser("alice@example.com");
        String householdId = createHousehold(token);

        given()
            .header("Authorization", "Bearer " + token)
            .multiPart("other", "irrelevant")
            .when().post("/api/v1/households/" + householdId + "/receipts/scan")
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

    private static String createHousehold(String token) {
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Test"))
            .when().post("/api/v1/households")
            .then().statusCode(201).extract().path("id");
    }
}
