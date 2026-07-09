package de.eateasy.auth.google;

/**
 * Verifiziert ein Google-ID-Token und liefert die enthaltenen Claims. Als
 * Interface gehalten, damit die Auth-Logik in Tests ohne echten Google-Aufruf
 * gemockt werden kann (analog {@code OcrClient}/{@code OllamaClient}).
 */
public interface GoogleTokenVerifier {

    /**
     * Prüft Signatur, Gültigkeit und Audience des Tokens.
     *
     * @throws de.eateasy.common.exception.InvalidCredentialsException wenn das
     *     Token fehlt, ungültig ist oder nicht für diese Anwendung ausgestellt
     *     wurde.
     */
    GoogleIdTokenPayload verify(String idToken);
}
