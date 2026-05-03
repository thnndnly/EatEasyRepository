package de.eateasy.household.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInvitationRequest(
    @NotBlank String token
) {
}
