package de.eateasy.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record RecipeCreateRequest(
    @NotBlank @Size(max = 200) String title,
    String description,
    @NotBlank String instructions,
    @Min(value = 1, message = "Portionen muss mindestens 1 sein") int servings,
    @Min(0) Integer prepMinutes,
    List<String> dietTags,
    UUID householdId,
    @NotEmpty(message = "Mindestens eine Zutat erforderlich") @Valid List<RecipeIngredientRequest> ingredients
) {
}
