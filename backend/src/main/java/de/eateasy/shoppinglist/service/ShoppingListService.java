package de.eateasy.shoppinglist.service;

import de.eateasy.shoppinglist.dto.ShoppingListDto;
import de.eateasy.shoppinglist.dto.ShoppingListItemDto;

import java.util.UUID;

public interface ShoppingListService {

    /**
     * Liefert die Einkaufsliste für den Wochenplan. Wenn noch keine existiert,
     * wird sie aus Plan minus Vorrat berechnet und gespeichert.
     */
    ShoppingListDto getOrGenerate(UUID userId, UUID mealPlanId);

    /**
     * Berechnet die Liste komplett neu. {@code checked}-Status bestehender
     * Einträge wird per (ingredientId, unit) auf die neuen Einträge
     * übertragen, soweit das Paar noch in der berechneten Liste vorkommt.
     */
    ShoppingListDto regenerate(UUID userId, UUID mealPlanId);

    /** Toggelt das Häckchen eines einzelnen Eintrags. */
    ShoppingListItemDto toggleChecked(UUID userId, UUID itemId, boolean checked);
}
