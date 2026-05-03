package de.eateasy.recipe.dto;

import de.eateasy.common.units.Unit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Eingabe-DTO fuer eine Recipe-Zutat. Entweder {@code ingredientId} (existierende
 * Zutat) ODER {@code ingredientName} (neue Zutat — wird via findOrCreate
 * angelegt). Der Service entscheidet, welcher Pfad genutzt wird.
 */
public record RecipeIngredientRequest(
    UUID ingredientId,
    @Size(max = 100) String ingredientName,
    @NotNull @DecimalMin(value = "0.01", message = "Menge muss positiv sein") BigDecimal amount,
    @NotNull Unit unit,
    @Size(max = 200) String note
) {
}
