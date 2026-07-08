package de.eateasy.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login per Google: das im Frontend (Google Identity Services) erhaltene
 * ID-Token wird zur serverseitigen Verifikation durchgereicht.
 */
public record GoogleLoginRequest(
    @NotBlank String idToken
) {
}
