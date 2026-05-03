package de.eateasy.common.security;

import de.eateasy.common.exception.InvalidCredentialsException;
import jakarta.enterprise.context.RequestScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

/**
 * Liefert die UUID des eingeloggten Users aus dem JWT-Claim "uid".
 * Wird von REST-Resources injiziert, damit der Aufrufer keinen JsonWebToken-
 * Boilerplate mehr braucht.
 */
@RequestScoped
public class CurrentUser {

    private static final String CLAIM_USER_ID = "uid";

    private final JsonWebToken jwt;

    public CurrentUser(JsonWebToken jwt) {
        this.jwt = jwt;
    }

    public UUID id() {
        String uid = jwt.getClaim(CLAIM_USER_ID);
        if (uid == null || uid.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return UUID.fromString(uid);
    }
}
