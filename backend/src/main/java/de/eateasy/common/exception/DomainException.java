package de.eateasy.common.exception;

/**
 * Basis fuer Fachfehler, die der Mapper auf konkrete HTTP-Status uebersetzt.
 * Kein direktes Werfen — immer eine Sub-Klasse benutzen.
 */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
