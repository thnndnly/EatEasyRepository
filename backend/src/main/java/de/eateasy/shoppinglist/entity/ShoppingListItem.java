package de.eateasy.shoppinglist.entity;

import de.eateasy.common.units.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "shopping_list_item")
public class ShoppingListItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingList shoppingList;

    @Column(name = "ingredient_id", nullable = false)
    private UUID ingredientId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 20)
    private Unit unit;

    @Column(name = "checked", nullable = false)
    private boolean checked;

    protected ShoppingListItem() {
    }

    public ShoppingListItem(UUID ingredientId, BigDecimal amount, Unit unit, boolean checked) {
        this.id = UUID.randomUUID();
        this.ingredientId = ingredientId;
        this.amount = normalize(amount);
        this.unit = unit;
        this.checked = checked;
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public ShoppingList getShoppingList() {
        return shoppingList;
    }

    void setShoppingList(ShoppingList shoppingList) {
        this.shoppingList = shoppingList;
    }

    public UUID getIngredientId() {
        return ingredientId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Unit getUnit() {
        return unit;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
