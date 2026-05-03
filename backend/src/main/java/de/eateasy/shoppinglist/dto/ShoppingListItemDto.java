package de.eateasy.shoppinglist.dto;

import de.eateasy.common.units.Unit;
import de.eateasy.shoppinglist.entity.ShoppingListItem;

import java.math.BigDecimal;
import java.util.UUID;

public record ShoppingListItemDto(
    UUID id,
    UUID ingredientId,
    String ingredientName,
    BigDecimal amount,
    Unit unit,
    boolean checked
) {
    public static ShoppingListItemDto from(ShoppingListItem item, String ingredientName) {
        return new ShoppingListItemDto(
            item.getId(),
            item.getIngredientId(),
            ingredientName,
            item.getAmount(),
            item.getUnit(),
            item.isChecked());
    }
}
