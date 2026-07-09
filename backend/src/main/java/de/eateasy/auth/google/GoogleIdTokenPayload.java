package de.eateasy.auth.google;

/**
 * Die für den Login relevanten Claims aus einem verifizierten Google-ID-Token.
 */
public record GoogleIdTokenPayload(
    String email,
    boolean emailVerified,
    String sub,
    String name
) {
}
