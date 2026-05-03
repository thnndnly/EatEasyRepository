package de.eateasy.household.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitationCreateRequest(
    @NotBlank @Email @Size(max = 255) String email
) {
}
