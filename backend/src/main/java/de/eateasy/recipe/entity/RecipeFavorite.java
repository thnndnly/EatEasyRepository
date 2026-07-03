package de.eateasy.recipe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/** Favoriten-Markierung: ein User hat ein Rezept als Favorit gespeichert. */
@Entity
@Table(name = "user_favorite_recipe",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "recipe_id"}))
public class RecipeFavorite {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "recipe_id", nullable = false, updatable = false)
    private UUID recipeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecipeFavorite() {
    }

    public RecipeFavorite(UUID userId, UUID recipeId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.recipeId = recipeId;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRecipeId() {
        return recipeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
