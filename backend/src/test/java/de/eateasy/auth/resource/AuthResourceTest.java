package de.eateasy.auth.resource;

import de.eateasy.auth.repository.UserRepository;
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
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthResourceTest {

    @Inject
    UserRepository userRepository;

    @BeforeEach
    @Transactional
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /auth/register liefert 201 + Token + User")
    void registerCreatesUser() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", "bob@example.com",
                "password", "secret12",
                "displayName", "Bob"))
            .when().post("/api/v1/auth/register")
            .then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("user.email", equalTo("bob@example.com"))
                .body("user.displayName", equalTo("Bob"));
    }

    @Test
    @DisplayName("POST /auth/register liefert 409 bei doppelter Email")
    void registerDuplicateEmail() {
        registerUser("dup@example.com", "secret12", "Dup");

        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", "dup@example.com",
                "password", "secret12",
                "displayName", "Dup 2"))
            .when().post("/api/v1/auth/register")
            .then()
                .statusCode(409)
                .body("error", notNullValue());
    }

    @Test
    @DisplayName("POST /auth/register liefert 400 bei zu kurzem Passwort")
    void registerInvalidPayload() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", "short@example.com",
                "password", "1234",
                "displayName", "Short"))
            .when().post("/api/v1/auth/register")
            .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("POST /auth/login mit korrektem Passwort liefert 200 + Token")
    void loginValid() {
        registerUser("carol@example.com", "secret12", "Carol");

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "carol@example.com", "password", "secret12"))
            .when().post("/api/v1/auth/login")
            .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("user.email", equalTo("carol@example.com"));
    }

    @Test
    @DisplayName("POST /auth/login mit falschem Passwort liefert 401")
    void loginInvalid() {
        registerUser("dave@example.com", "secret12", "Dave");

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "dave@example.com", "password", "wrong"))
            .when().post("/api/v1/auth/login")
            .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST /auth/google liefert 404, wenn Google-OAuth deaktiviert ist (Default)")
    void googleLoginDisabledReturns404() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("idToken", "irrelevant"))
            .when().post("/api/v1/auth/google")
            .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("GET /auth/me ohne Token liefert 401")
    void meUnauthenticated() {
        given()
            .when().get("/api/v1/auth/me")
            .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("GET /auth/me mit gültigem Token liefert User")
    void meAuthenticated() {
        String token = registerUser("eve@example.com", "secret12", "Eve");

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/v1/auth/me")
            .then()
                .statusCode(200)
                .body("email", equalTo("eve@example.com"))
                .body("displayName", equalTo("Eve"));
    }

    private static String registerUser(String email, String password, String displayName) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", password, "displayName", displayName))
            .when().post("/api/v1/auth/register")
            .then().statusCode(201)
            .extract().path("token");
    }
}
