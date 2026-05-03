package de.eateasy.mealplan.dto;

import de.eateasy.mealplan.entity.MealPlanEntry;
import de.eateasy.mealplan.entity.MealType;
import de.eateasy.recipe.dto.RecipeMiniDto;

import java.time.DayOfWeek;
import java.util.UUID;

/**
 * Wochenplan-Slot mit eingebettetem Recipe-Mini-DTO. Wenn das referenzierte
 * Rezept nicht (mehr) auflösbar ist, ist {@code recipe} null und das Frontend
 * zeigt einen Platzhalter.
 */
public record MealPlanEntryDto(
    UUID id,
    DayOfWeek dayOfWeek,
    MealType mealType,
    int servings,
    RecipeMiniDto recipe
) {
    public static MealPlanEntryDto from(MealPlanEntry entry, RecipeMiniDto recipe) {
        return new MealPlanEntryDto(
            entry.getId(),
            entry.getDayOfWeek(),
            entry.getMealType(),
            entry.getServings(),
            recipe);
    }
}
