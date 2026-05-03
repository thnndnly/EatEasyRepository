package de.eateasy.recipe.dto;

import de.eateasy.common.units.Unit;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Minimaler View auf eine Recipe-Zutat — fuer Aggregations-Pipelines wie
 * Einkaufsliste und Smart-Suggestion. Enthaelt nur das, was fuer Mengen-
 * Rechnung gebraucht wird, ohne Ingredient-Name (kann der Aufrufer separat
 * via {@link de.eateasy.ingredient.service.IngredientService#getByIds} laden).
 */
public record RecipeIngredientView(
    UUID ingredientId,
    BigDecimal amount,
    Unit unit
) {
}
