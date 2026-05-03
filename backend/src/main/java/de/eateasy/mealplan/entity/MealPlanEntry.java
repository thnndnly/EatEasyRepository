package de.eateasy.mealplan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.util.UUID;

@Entity
@Table(name = "meal_plan_entry")
public class MealPlanEntry {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    /**
     * Cross-Component-FK auf {@code recipe.id}. Der MealPlanService loest die
     * Mini-Daten ueber {@link de.eateasy.recipe.service.RecipeService} auf,
     * damit die Komponenten-Grenze sauber bleibt.
     */
    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(name = "servings", nullable = false)
    private int servings;

    protected MealPlanEntry() {
    }

    public MealPlanEntry(DayOfWeek dayOfWeek, MealType mealType, UUID recipeId, int servings) {
        this.id = UUID.randomUUID();
        this.dayOfWeek = dayOfWeek;
        this.mealType = mealType;
        this.recipeId = recipeId;
        this.servings = servings;
    }

    public UUID getId() {
        return id;
    }

    public MealPlan getMealPlan() {
        return mealPlan;
    }

    void setMealPlan(MealPlan mealPlan) {
        this.mealPlan = mealPlan;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public MealType getMealType() {
        return mealType;
    }

    public UUID getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(UUID recipeId) {
        this.recipeId = recipeId;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }
}
