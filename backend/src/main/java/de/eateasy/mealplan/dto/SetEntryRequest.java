package de.eateasy.mealplan.dto;

import de.eateasy.mealplan.entity.MealType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.util.UUID;

/**
 * PUT-Body fuer einen Wochenplan-Slot. Idempotent: existierender Eintrag
 * wird ueberschrieben (Unique-Constraint auf plan x day x mealType).
 */
public record SetEntryRequest(
    @NotNull DayOfWeek dayOfWeek,
    @NotNull MealType mealType,
    @NotNull UUID recipeId,
    @Min(value = 1, message = "Portionen muss mindestens 1 sein") int servings
) {
}
