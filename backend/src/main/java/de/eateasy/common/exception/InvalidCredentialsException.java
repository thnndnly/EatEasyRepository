package de.eateasy.common.exception;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Ungültige Login-Daten");
    }
}
