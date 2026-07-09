package de.eateasy.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Komplett-Update eines Rezepts. Vollständig gespiegelte Eingabe — wir machen
 * keinen Patch-Modus, weil Zutatenliste sonst inkonsistent wäre.
 */
public record RecipeUpdateRequest(
    @NotBlank @Size(max = 200) String title,
    String description,
    @NotBlank String instructions,
    @Min(value = 1) int servings,
    @Min(0) Integer prepMinutes,
    List<String> dietTags,
    UUID householdId,
    @NotEmpty @Valid List<RecipeIngredientRequest> ingredients
) {
}
