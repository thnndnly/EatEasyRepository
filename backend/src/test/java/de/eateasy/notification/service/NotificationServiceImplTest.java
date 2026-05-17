package de.eateasy.notification.service;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class NotificationServiceImplTest {

    @Inject
    NotificationService notificationService;

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void clearMails() {
        mailbox.clear();
    }

    @Test
    @DisplayName("sendInvitation schickt HTML-Mail mit Token-Link und Haushalts-Name")
    void sendInvitation() {
        notificationService.sendInvitation(
            "bob@example.com",
            "Test-WG",
            "Alice Owner",
            "tok-XYZ123");

        var sent = mailbox.getMessagesSentTo("bob@example.com");
        assertThat(sent).hasSize(1);
        var mail = sent.get(0);
        assertThat(mail.getSubject()).contains("Test-WG");
        assertThat(mail.getHtml()).contains("Test-WG");
        assertThat(mail.getHtml()).contains("Alice Owner");
        assertThat(mail.getHtml()).contains("token=tok-XYZ123");
    }

    @Test
    @DisplayName("Token-Link wird URL-encoded — Sonderzeichen brechen die URL nicht")
    void urlEncodesToken() {
        notificationService.sendInvitation(
            "bob@example.com",
            "WG",
            "Alice",
            "abc+/=");

        var sent = mailbox.getMessagesSentTo("bob@example.com");
        assertThat(sent).hasSize(1);
        // '+' und '/' werden encoded; '=' bleibt geht aber bei UrlEncoder als %3D
        assertThat(sent.get(0).getHtml()).contains("token=abc%2B%2F%3D");
    }

    @Test
    @DisplayName("Subject enthaelt 'EatEasy'-Markenbezeichnung")
    void subjectMentionsBrand() {
        notificationService.sendInvitation(
            "bob@example.com", "WG", "Alice", "tok");

        List<io.quarkus.mailer.Mail> sent = mailbox.getMessagesSentTo("bob@example.com");
        assertThat(sent.get(0).getSubject()).contains("EatEasy");
    }
}
