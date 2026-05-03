package de.eateasy.household.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HouseholdCreateRequest(
    @NotBlank @Size(max = 100) String name,
    List<String> defaultDietTags
) {
}
