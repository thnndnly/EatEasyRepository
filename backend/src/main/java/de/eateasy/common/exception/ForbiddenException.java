package de.eateasy.common.exception;

/**
 * Wirft, wenn der eingeloggte User keine Berechtigung fuer eine Ressource hat.
 * Mapper uebersetzt auf HTTP 403.
 */
public class ForbiddenException extends DomainException {
    public ForbiddenException(String message) {
        super(message);
    }
}
