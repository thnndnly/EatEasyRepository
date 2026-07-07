package de.eateasy.shoppinglist.dto;

import de.eateasy.common.units.Unit;
import de.eateasy.ingredient.entity.IngredientCategory;
import de.eateasy.shoppinglist.entity.ShoppingListItem;

import java.math.BigDecimal;
import java.util.UUID;

public record ShoppingListItemDto(
    UUID id,
    UUID ingredientId,
    String ingredientName,
    IngredientCategory category,
    BigDecimal amount,
    Unit unit,
    boolean checked
) {
    public static ShoppingListItemDto from(ShoppingListItem item, String ingredientName,
                                           IngredientCategory category) {
        return new ShoppingListItemDto(
            item.getId(),
            item.getIngredientId(),
            ingredientName,
            category,
            item.getAmount(),
            item.getUnit(),
            item.isChecked());
    }
}
