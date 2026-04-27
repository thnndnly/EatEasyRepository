package de.eateasy.common.exception;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Ungueltige Login-Daten");
    }
}
