package de.eateasy.ingredient.dto;

import de.eateasy.common.units.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IngredientCreateRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull Unit defaultUnit
) {
}
