package de.eateasy.recipe.dto;

import de.eateasy.recipe.entity.Recipe;

import java.util.List;
import java.util.UUID;

/**
 * Kompakte Sicht auf ein Rezept für eingebettete Anzeige in Wochenplan-Slots,
 * Smart-Suggestions usw. Bewusst ohne Zutaten / Instructions, um die Payload
 * klein zu halten und N+1-Lookups zu vermeiden.
 */
public record RecipeMiniDto(
    UUID id,
    String title,
    int servings,
    Integer prepMinutes,
    List<String> dietTags
) {
    public static RecipeMiniDto from(Recipe recipe) {
        return new RecipeMiniDto(
            recipe.getId(),
            recipe.getTitle(),
            recipe.getServings(),
            recipe.getPrepMinutes(),
            List.of(recipe.getDietTags()));
    }
}
