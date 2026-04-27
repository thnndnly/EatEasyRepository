package de.eateasy.common;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class HealthResourceTest {

    @Test
    void healthEndpointReturnsOk() {
        given()
            .when().get("/api/v1/health")
            .then()
                .statusCode(200)
                .body("status", equalTo("ok"));
    }
}
