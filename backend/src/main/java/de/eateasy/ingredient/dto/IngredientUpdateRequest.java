package de.eateasy.ingredient.dto;

import de.eateasy.ingredient.entity.IngredientCategory;
import jakarta.validation.constraints.NotNull;

/** Aktuell einziges editierbares Feld: die Supermarkt-Kategorie (Phase 16). */
public record IngredientUpdateRequest(
    @NotNull IngredientCategory category
) {
}
