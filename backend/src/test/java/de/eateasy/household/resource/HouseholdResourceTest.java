package de.eateasy.household.resource;

import de.eateasy.auth.repository.UserRepository;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class HouseholdResourceTest {

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
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /households ohne Token liefert 401")
    void createUnauthenticated() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Test"))
            .when().post("/api/v1/households")
            .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST /households legt Haushalt an und liefert 201")
    void createHousehold() {
        String token = registerUser("alice@example.com", "Alice");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Familie", "defaultDietTags", List.of("vegetarian")))
            .when().post("/api/v1/households")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Familie"))
                .body("role", equalTo("OWNER"))
                .body("defaultDietTags", hasItem("vegetarian"))
                // Auto-Nachbuchen ist bei neuen Haushalten standardmaessig an.
                .body("autoRestockEnabled", equalTo(true));
    }

    @Test
    @DisplayName("PATCH /households/{id} als Owner schaltet Auto-Nachbuchen ab (persistiert)")
    void patchDisablesAutoRestock() {
        String ownerToken = registerUser("owner@example.com", "Owner");
        String householdId = createHousehold(ownerToken, "Test");

        given()
            .header("Authorization", "Bearer " + ownerToken)
            .contentType(ContentType.JSON)
            .body(Map.of("autoRestockEnabled", false))
            .when().patch("/api/v1/households/" + householdId)
            .then()
                .statusCode(200)
                .body("autoRestockEnabled", equalTo(false));

        // Persistiert: erneutes GET liefert weiterhin false.
        given()
            .header("Authorization", "Bearer " + ownerToken)
            .when().get("/api/v1/households/" + householdId)
            .then()
                .statusCode(200)
                .body("autoRestockEnabled", equalTo(false));
    }

    @Test
    @DisplayName("POST /households mit unbekanntem Diaet-Tag liefert 400")
    void createHouseholdRejectsUnknownDietTag() {
        String token = registerUser("alice@example.com", "Alice");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Familie", "defaultDietTags", List.of("paleo")))
            .when().post("/api/v1/households")
            .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /households liefert nur Haushalte des Aufrufers")
    void listOwnHouseholdsOnly() {
        String aliceToken = registerUser("alice@example.com", "Alice");
        String bobToken = registerUser("bob@example.com", "Bob");
        createHousehold(aliceToken, "Alice Haus");
        createHousehold(bobToken, "Bob Haus");

        given()
            .header("Authorization", "Bearer " + aliceToken)
            .when().get("/api/v1/households")
            .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo("Alice Haus"));
    }

    @Test
    @DisplayName("GET /households/{id} liefert 403 fuer Nicht-Mitglieder")
    void getForbiddenForOutsider() {
        String aliceToken = registerUser("alice@example.com", "Alice");
        String bobToken = registerUser("bob@example.com", "Bob");
        String householdId = createHousehold(aliceToken, "Alice Haus");

        given()
            .header("Authorization", "Bearer " + bobToken)
            .when().get("/api/v1/households/" + householdId)
            .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("PATCH /households/{id} als Member liefert 403")
    void patchAsMemberForbidden() {
        String ownerToken = registerUser("owner@example.com", "Owner");
        String memberToken = registerUser("member@example.com", "Member");
        String householdId = createHousehold(ownerToken, "Test");
        String invitationToken = invite(ownerToken, householdId, "member@example.com");
        accept(memberToken, invitationToken);

        given()
            .header("Authorization", "Bearer " + memberToken)
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Hijack"))
            .when().patch("/api/v1/households/" + householdId)
            .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Invitation-Flow: Einladen, Annehmen, Mitgliederliste enthaelt beide")
    void invitationFlow() {
        String ownerToken = registerUser("owner@example.com", "Owner");
        String guestToken = registerUser("guest@example.com", "Guest");
        String householdId = createHousehold(ownerToken, "Familie");

        String invitationToken = invite(ownerToken, householdId, "guest@example.com");
        accept(guestToken, invitationToken);

        given()
            .header("Authorization", "Bearer " + ownerToken)
            .when().get("/api/v1/households/" + householdId + "/members")
            .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("email", hasItem("guest@example.com"));
    }

    @Test
    @DisplayName("acceptInvitation mit fremder Email liefert 403")
    void acceptWrongEmail() {
        String ownerToken = registerUser("owner@example.com", "Owner");
        String otherToken = registerUser("other@example.com", "Other");
        String householdId = createHousehold(ownerToken, "Test");
        String invitationToken = invite(ownerToken, householdId, "guest@example.com");

        given()
            .header("Authorization", "Bearer " + otherToken)
            .contentType(ContentType.JSON)
            .body(Map.of("token", invitationToken))
            .when().post("/api/v1/invitations/accept")
            .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("DELETE /members/{userId} entfernt das Mitglied")
    void removeMember() {
        String ownerToken = registerUser("owner@example.com", "Owner");
        String memberToken = registerUser("member@example.com", "Member");
        String householdId = createHousehold(ownerToken, "Test");
        String invitationToken = invite(ownerToken, householdId, "member@example.com");
        accept(memberToken, invitationToken);

        String memberId = given()
            .header("Authorization", "Bearer " + memberToken)
            .when().get("/api/v1/auth/me")
            .then().statusCode(200)
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + ownerToken)
            .when().delete("/api/v1/households/" + householdId + "/members/" + memberId)
            .then()
                .statusCode(204);

        // Der entfernte User sieht den Haushalt nicht mehr.
        given()
            .header("Authorization", "Bearer " + memberToken)
            .when().get("/api/v1/households")
            .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    // --- Helpers ---------------------------------------------------------

    private static String registerUser(String email, String displayName) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", "secret12", "displayName", displayName))
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

    private static String invite(String ownerToken, String householdId, String email) {
        return given()
            .header("Authorization", "Bearer " + ownerToken)
            .contentType(ContentType.JSON)
            .body(Map.of("email", email))
            .when().post("/api/v1/households/" + householdId + "/invitations")
            .then().statusCode(201)
            .extract().path("token");
    }

    private static void accept(String guestToken, String invitationToken) {
        given()
            .header("Authorization", "Bearer " + guestToken)
            .contentType(ContentType.JSON)
            .body(Map.of("token", invitationToken))
            .when().post("/api/v1/invitations/accept")
            .then().statusCode(200);
    }
}
