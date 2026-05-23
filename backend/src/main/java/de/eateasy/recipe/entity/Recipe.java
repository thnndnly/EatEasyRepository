package de.eateasy.recipe.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "recipe")
public class Recipe {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /** NULL = privates Rezept; sonst sichtbar fuer alle Mitglieder. */
    @Column(name = "household_id")
    private UUID householdId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "instructions", nullable = false, columnDefinition = "text")
    private String instructions;

    @Column(name = "servings", nullable = false)
    private int servings;

    @Column(name = "prep_minutes")
    private Integer prepMinutes;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "diet_tags", nullable = false)
    private String[] dietTags;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "external_source", length = 50)
    private String externalSource;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
        mappedBy = "recipe",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY)
    @OrderBy("ingredientId asc")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    protected Recipe() {
    }

    public Recipe(UUID ownerId,
                  UUID householdId,
                  String title,
                  String description,
                  String instructions,
                  int servings,
                  Integer prepMinutes,
                  String[] dietTags,
                  String sourceUrl,
                  String externalSource) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.householdId = householdId;
        this.title = title;
        this.description = description;
        this.instructions = instructions;
        this.servings = servings;
        this.prepMinutes = prepMinutes;
        this.dietTags = dietTags == null ? new String[0] : dietTags.clone();
        this.sourceUrl = sourceUrl;
        this.externalSource = externalSource;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (dietTags == null) {
            dietTags = new String[0];
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(UUID householdId) {
        this.householdId = householdId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public Integer getPrepMinutes() {
        return prepMinutes;
    }

    public void setPrepMinutes(Integer prepMinutes) {
        this.prepMinutes = prepMinutes;
    }

    public String[] getDietTags() {
        return dietTags == null ? new String[0] : dietTags.clone();
    }

    public void setDietTags(String[] dietTags) {
        this.dietTags = dietTags == null ? new String[0] : dietTags.clone();
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    /** Loescht alle bisherigen Zutaten und uebernimmt die uebergebenen. Nutzt
     *  orphanRemoval, damit verwaiste Eintraege automatisch geloescht werden. */
    public void replaceIngredients(List<RecipeIngredient> next) {
        this.ingredients.clear();
        for (RecipeIngredient ri : next) {
            ri.setRecipe(this);
            this.ingredients.add(ri);
        }
    }
}
