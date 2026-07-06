package de.eateasy.common.exception;

/**
 * Wirft, wenn ein nachgelagerter Dienst (z. B. OCR/Tesseract) nicht erreichbar
 * ist oder unerwartet antwortet. Mapper uebersetzt auf HTTP 503 — der Aufrufer
 * darf es spaeter erneut versuchen. Die Detail-Ursache wird serverseitig
 * geloggt, die Nachricht an den Client bleibt generisch.
 */
public class ServiceUnavailableException extends DomainException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
