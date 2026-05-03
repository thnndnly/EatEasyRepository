package de.eateasy.shoppinglist.dto;

import de.eateasy.shoppinglist.entity.ShoppingList;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShoppingListDto(
    UUID id,
    UUID householdId,
    UUID mealPlanId,
    List<ShoppingListItemDto> items,
    Instant updatedAt
) {
    public static ShoppingListDto from(ShoppingList list, List<ShoppingListItemDto> items) {
        return new ShoppingListDto(
            list.getId(),
            list.getHouseholdId(),
            list.getMealPlanId(),
            items,
            list.getUpdatedAt());
    }
}
