package de.eateasy.mealplan.service;

import de.eateasy.mealplan.dto.MealPlanDto;
import de.eateasy.mealplan.dto.MealPlanEntryDto;
import de.eateasy.mealplan.dto.SetEntryRequest;
import de.eateasy.mealplan.entity.MealType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

public interface MealPlanService {

    /**
     * Liefert den Wochenplan fuer die Woche, in die {@code anyDateInWeek} faellt.
     * Wenn noch kein Plan existiert, wird er lazy angelegt. {@code anyDateInWeek}
     * darf jeder Wochentag sein — der Service normalisiert auf Montag.
     */
    MealPlanDto getOrCreate(UUID userId, UUID householdId, LocalDate anyDateInWeek);

    /** Liefert den Plan der aktuellen Kalenderwoche; legt ihn lazy an. */
    MealPlanDto getCurrent(UUID userId, UUID householdId);

    /** Setzt einen Slot — vorhandener Eintrag wird ersetzt. */
    MealPlanEntryDto setEntry(UUID userId, UUID mealPlanId, SetEntryRequest request);

    /** Loescht einen Slot. Idempotent: 204 auch wenn er nicht da war. */
    void removeEntry(UUID userId, UUID mealPlanId, DayOfWeek day, MealType mealType);
}
