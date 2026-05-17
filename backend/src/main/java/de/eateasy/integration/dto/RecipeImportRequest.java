package de.eateasy.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RecipeImportRequest(
    @NotBlank @Size(max = 50) String source,
    @NotBlank @Size(max = 100) String externalId,
    UUID householdId
) {
}
