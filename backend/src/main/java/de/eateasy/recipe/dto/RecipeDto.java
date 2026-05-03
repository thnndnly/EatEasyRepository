package de.eateasy.recipe.dto;

import de.eateasy.recipe.entity.Recipe;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecipeDto(
    UUID id,
    UUID ownerId,
    UUID householdId,
    String title,
    String description,
    String instructions,
    int servings,
    Integer prepMinutes,
    List<String> dietTags,
    String sourceUrl,
    String externalSource,
    List<RecipeIngredientDto> ingredients,
    Instant createdAt,
    Instant updatedAt
) {
    public static RecipeDto from(Recipe recipe, List<RecipeIngredientDto> ingredients) {
        return new RecipeDto(
            recipe.getId(),
            recipe.getOwnerId(),
            recipe.getHouseholdId(),
            recipe.getTitle(),
            recipe.getDescription(),
            recipe.getInstructions(),
            recipe.getServings(),
            recipe.getPrepMinutes(),
            List.of(recipe.getDietTags()),
            recipe.getSourceUrl(),
            recipe.getExternalSource(),
            ingredients,
            recipe.getCreatedAt(),
            recipe.getUpdatedAt());
    }
}
