package de.eateasy.common.exception;

/**
 * Wirft, wenn fachliche Validierung fehlschlaegt (jenseits von Bean Validation).
 * Mapper uebersetzt auf HTTP 400.
 */
public class BadRequestException extends DomainException {
    public BadRequestException(String message) {
        super(message);
    }
}
