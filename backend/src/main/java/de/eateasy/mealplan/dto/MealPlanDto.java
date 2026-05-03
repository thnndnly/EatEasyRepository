package de.eateasy.mealplan.dto;

import de.eateasy.mealplan.entity.MealPlan;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MealPlanDto(
    UUID id,
    UUID householdId,
    LocalDate weekStart,
    List<MealPlanEntryDto> entries
) {
    public static MealPlanDto from(MealPlan plan, List<MealPlanEntryDto> entries) {
        return new MealPlanDto(plan.getId(), plan.getHouseholdId(), plan.getWeekStart(), entries);
    }
}
