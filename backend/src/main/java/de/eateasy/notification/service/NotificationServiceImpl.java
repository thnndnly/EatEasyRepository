package de.eateasy.notification.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationServiceImpl.class);

    private final Mailer mailer;
    private final Template invitationTemplate;
    private final String frontendUrl;

    public NotificationServiceImpl(Mailer mailer,
                                   @Location("invitation.html") Template invitationTemplate,
                                   @ConfigProperty(name = "app.frontend.url") String frontendUrl) {
        this.mailer = mailer;
        this.invitationTemplate = invitationTemplate;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void sendInvitation(String recipientEmail, String householdName,
                               String inviterName, String token) {
        String acceptUrl = frontendUrl
            + "/invitations/accept?token="
            + URLEncoder.encode(token, StandardCharsets.UTF_8);

        try {
            String html = invitationTemplate
                .data("householdName", householdName)
                .data("inviterName", inviterName)
                .data("acceptUrl", acceptUrl)
                .render();

            // householdName fliesst in den Subject-Header ein; der Quarkus Mailer
            // (Vert.x Mail) kodiert Header-Werte selbst und schuetzt so vor
            // Header-Injection durch Steuerzeichen im Namen.
            String subject = "Einladung zu " + householdName + " bei EatEasy";
            mailer.send(Mail.withHtml(recipientEmail, subject, html));
        } catch (Exception ex) {
            LOG.warnf(ex, "Einladungsmail an %s konnte nicht verschickt werden — "
                + "Invitation bleibt aber persistiert (Token kann manuell weitergegeben werden)",
                recipientEmail);
        }
    }
}
