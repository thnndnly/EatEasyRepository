package de.eateasy.notification;

import de.eateasy.auth.repository.UserRepository;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class InvitationMailIntegrationTest {

    @Inject
    MockMailbox mailbox;

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
        mailbox.clear();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /households/{id}/invitations verschickt Mail mit Token-Link")
    void inviteSendsMail() {
        String aliceToken = registerUser("alice@example.com", "Alice Owner");
        String householdId = createHousehold(aliceToken, "Alice-WG");

        String invitedEmail = "bob@example.com";
        String mailToken = given()
            .header("Authorization", "Bearer " + aliceToken)
            .contentType(ContentType.JSON)
            .body(Map.of("email", invitedEmail))
            .when().post("/api/v1/households/" + householdId + "/invitations")
            .then().statusCode(201)
            .extract().path("token");

        var sent = mailbox.getMessagesSentTo(invitedEmail);
        assertThat(sent).hasSize(1);
        var mail = sent.get(0);
        assertThat(mail.getSubject()).contains("Alice-WG");
        assertThat(mail.getHtml()).contains("Alice-WG");
        assertThat(mail.getHtml()).contains("Alice Owner");
        assertThat(mail.getHtml()).contains("token=" + mailToken);
    }

    // --- Helpers ---------------------------------------------------------

    private static String registerUser(String email, String displayName) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", "secret12", "displayName", displayName))
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
