package de.eateasy.common.exception;

/**
 * Wirft, wenn eine Operation gegen einen fachlichen Konflikt laeuft
 * (z. B. User ist bereits Mitglied, Einladung schon eingeloest).
 * Mapper uebersetzt auf HTTP 409.
 */
public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }
}
