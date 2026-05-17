package de.eateasy.notification.service;

public interface NotificationService {

    /**
     * Verschickt eine Haushaltseinladung per E-Mail. Fehler beim Mail-Versand
     * werden nur geloggt und nicht propagiert — die Invitation selbst ist
     * bereits persistiert, der Token kann notfalls aus dem Backend gelesen
     * werden.
     */
    void sendInvitation(String recipientEmail, String householdName,
                        String inviterName, String token);
}
