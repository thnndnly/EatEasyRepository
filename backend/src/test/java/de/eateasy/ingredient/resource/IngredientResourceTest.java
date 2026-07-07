package de.eateasy.ingredient.resource;

import de.eateasy.auth.repository.UserRepository;
import de.eateasy.ingredient.repository.IngredientRepository;
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

@QuarkusTest
class IngredientResourceTest {

    @Inject
    IngredientRepository ingredientRepository;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    @Transactional
    void cleanUp() {
        ingredientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("PATCH /ingredients/{id} ohne Token liefert 401")
    void updateUnauthenticated() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("category", "VORRAT"))
            .when().patch("/api/v1/ingredients/" + UUID.randomUUID())
            .then().statusCode(401);
    }

    @Test
    @DisplayName("POST legt Zutat mit SONSTIGES an, PATCH setzt Kategorie")
    void createThenUpdateCategory() {
        String token = registerUser("alice@example.com");

        String id = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Tomate", "defaultUnit", "PIECE"))
            .when().post("/api/v1/ingredients")
            .then()
                .statusCode(201)
                .body("category", equalTo("SONSTIGES"))
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("category", "OBST_GEMUESE"))
            .when().patch("/api/v1/ingredients/" + id)
            .then()
                .statusCode(200)
                .body("category", equalTo("OBST_GEMUESE"));

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/v1/ingredients/" + id)
            .then()
                .statusCode(200)
                .body("category", equalTo("OBST_GEMUESE"));
    }

    @Test
    @DisplayName("PATCH mit unbekannter ID liefert 404")
    void updateUnknownIdNotFound() {
        String token = registerUser("alice@example.com");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("category", "VORRAT"))
            .when().patch("/api/v1/ingredients/" + UUID.randomUUID())
            .then().statusCode(404);
    }

    @Test
    @DisplayName("PATCH mit ungueltiger Kategorie liefert 4xx")
    void updateInvalidCategoryRejected() {
        String token = registerUser("alice@example.com");
        String id = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Tomate", "defaultUnit", "PIECE"))
            .when().post("/api/v1/ingredients")
            .then().statusCode(201).extract().path("id");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("category", "KEINE_ECHTE_KATEGORIE"))
            .when().patch("/api/v1/ingredients/" + id)
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
}
