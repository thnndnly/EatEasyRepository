package de.eateasy.recipe.entity;

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
import java.util.UUID;

@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredient {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    /**
     * Cross-Component-FK auf {@code ingredient.id}. Bewusst KEIN ManyToOne —
     * der RecipeService loest die Zutat ueber {@code IngredientService} auf,
     * statt das Ingredient-Entity direkt zu kennen.
     */
    @Column(name = "ingredient_id", nullable = false)
    private UUID ingredientId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 20)
    private Unit unit;

    @Column(name = "note", length = 200)
    private String note;

    protected RecipeIngredient() {
    }

    public RecipeIngredient(UUID ingredientId, BigDecimal amount, Unit unit, String note) {
        this.id = UUID.randomUUID();
        this.ingredientId = ingredientId;
        this.amount = amount;
        this.unit = unit;
        this.note = note;
    }

    public UUID getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    void setRecipe(Recipe recipe) {
        this.recipe = recipe;
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

    public String getNote() {
        return note;
    }
}
