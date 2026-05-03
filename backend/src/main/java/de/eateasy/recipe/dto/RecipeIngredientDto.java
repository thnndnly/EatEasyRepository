package de.eateasy.recipe.dto;

import de.eateasy.common.units.Unit;
import de.eateasy.recipe.entity.RecipeIngredient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Antwort-DTO einer Recipe-Zutat. {@code ingredientName} wird vom Service
 * separat aufgeloest, damit das Frontend den Picker initial befuellen kann
 * ohne nachzuladen.
 */
public record RecipeIngredientDto(
    UUID id,
    UUID ingredientId,
    String ingredientName,
    BigDecimal amount,
    Unit unit,
    String note
) {
    public static RecipeIngredientDto from(RecipeIngredient ri, String ingredientName) {
        return new RecipeIngredientDto(
            ri.getId(),
            ri.getIngredientId(),
            ingredientName,
            ri.getAmount(),
            ri.getUnit(),
            ri.getNote());
    }
}
