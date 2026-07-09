package de.eateasy.common.exception;

/**
 * Wirft, wenn eine Operation gegen einen fachlichen Konflikt läuft
 * (z. B. User ist bereits Mitglied, Einladung schon eingelöst).
 * Mapper übersetzt auf HTTP 409.
 */
public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }
}
