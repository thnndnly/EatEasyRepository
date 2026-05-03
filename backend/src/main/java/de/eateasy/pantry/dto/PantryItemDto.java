package de.eateasy.pantry.dto;

import de.eateasy.common.units.Unit;
import de.eateasy.pantry.entity.PantryItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PantryItemDto(
    UUID id,
    UUID householdId,
    UUID ingredientId,
    String ingredientName,
    BigDecimal amount,
    Unit unit,
    LocalDate bestBefore
) {
    public static PantryItemDto from(PantryItem item, String ingredientName) {
        return new PantryItemDto(
            item.getId(),
            item.getHouseholdId(),
            item.getIngredientId(),
            ingredientName,
            item.getAmount(),
            item.getUnit(),
            item.getBestBefore());
    }
}
