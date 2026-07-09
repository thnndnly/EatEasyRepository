package de.eateasy.common.exception;

/**
 * Wirft, wenn fachliche Validierung fehlschlägt (jenseits von Bean Validation).
 * Mapper übersetzt auf HTTP 400.
 */
public class BadRequestException extends DomainException {
    public BadRequestException(String message) {
        super(message);
    }
}
