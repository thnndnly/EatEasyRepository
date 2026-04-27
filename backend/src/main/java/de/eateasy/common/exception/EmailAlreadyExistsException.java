package de.eateasy.common.exception;

public class EmailAlreadyExistsException extends DomainException {
    public EmailAlreadyExistsException(String email) {
        super("Email bereits registriert: " + email);
    }
}
