package de.eateasy.ingredient.dto;

import de.eateasy.common.units.Unit;
import de.eateasy.ingredient.entity.Ingredient;

import java.util.UUID;

public record IngredientDto(
    UUID id,
    String name,
    Unit defaultUnit
) {
    public static IngredientDto from(Ingredient ingredient) {
        return new IngredientDto(ingredient.getId(), ingredient.getName(), ingredient.getDefaultUnit());
    }
}
