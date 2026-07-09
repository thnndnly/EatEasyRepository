package de.eateasy.auth.resource;

import de.eateasy.auth.google.GoogleIdTokenPayload;
import de.eateasy.auth.google.GoogleTokenVerifier;
import de.eateasy.auth.repository.UserRepository;
import io.quarkus.test.InjectMock;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Feature-Flag an: {@code POST /auth/google} verifiziert das (gemockte)
 * Google-Token und liefert ein EatEasy-JWT. Der echte Google-Aufruf ist über
 * den gemockten {@link GoogleTokenVerifier} ausgeschaltet.
 */
@QuarkusTest
@TestProfile(AuthGoogleResourceTest.GoogleEnabledProfile.class)
class AuthGoogleResourceTest {

    public static class GoogleEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "eateasy.google-oauth.enabled", "true",
                "google.oauth.client-id", "test-client-id.apps.googleusercontent.com");
        }
    }

    @InjectMock
    GoogleTokenVerifier googleTokenVerifier;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    @Transactional
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Flag an: POST /auth/google liefert 200 + Token + User")
    void googleLoginReturnsToken() {
        when(googleTokenVerifier.verify(anyString())).thenReturn(
            new GoogleIdTokenPayload("frank@example.com", true, "google-sub-9", "Frank G"));

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("idToken", "fake-id-token"))
            .when().post("/api/v1/auth/google")
            .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("user.email", equalTo("frank@example.com"))
                .body("user.displayName", equalTo("Frank G"));
    }

    @Test
    @DisplayName("Flag an: leeres idToken liefert 400 (Bean Validation)")
    void googleLoginRejectsBlankToken() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("idToken", ""))
            .when().post("/api/v1/auth/google")
            .then()
                .statusCode(400);
    }
}
