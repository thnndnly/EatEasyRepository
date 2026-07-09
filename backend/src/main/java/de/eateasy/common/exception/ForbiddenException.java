package de.eateasy.common.exception;

/**
 * Wirft, wenn der eingeloggte User keine Berechtigung für eine Ressource hat.
 * Mapper übersetzt auf HTTP 403.
 */
public class ForbiddenException extends DomainException {
    public ForbiddenException(String message) {
        super(message);
    }
}
