package de.eateasy.receipt.resource;

import de.eateasy.auth.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Feature-Flag-Verhalten: mit {@code eateasy.receipt.enabled=false} tut der
 * Endpoint so, als gäbe es ihn nicht (404) — so läuft die Render-Demo ohne
 * Tesseract/Ollama-Container.
 */
@QuarkusTest
@TestProfile(ReceiptResourceDisabledTest.ReceiptDisabledProfile.class)
class ReceiptResourceDisabledTest {

    public static class ReceiptDisabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("eateasy.receipt.enabled", "false");
        }
    }

    @Inject
    UserRepository userRepository;

    @BeforeEach
    @Transactional
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Flag aus: POST /receipts/scan liefert 404 trotz gültigem Token")
    void scanReturns404WhenDisabled() {
        String token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "alice@example.com", "password", "secret12", "displayName", "Alice"))
            .when().post("/api/v1/auth/register")
            .then().statusCode(201).extract().path("token");

        given()
            .header("Authorization", "Bearer " + token)
            .multiPart("file", "bon.jpg", new byte[] {1, 2, 3}, "image/jpeg")
            .when().post("/api/v1/households/" + UUID.randomUUID() + "/receipts/scan")
            .then().statusCode(404);
    }
}
