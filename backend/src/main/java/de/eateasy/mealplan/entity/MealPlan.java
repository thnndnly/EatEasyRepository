package de.eateasy.mealplan.entity;

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

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meal_plan")
public class MealPlan {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    /** Immer ein Montag — Service normalisiert das Datum. */
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
        mappedBy = "mealPlan",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY)
    @OrderBy("dayOfWeek asc, mealType asc")
    private List<MealPlanEntry> entries = new ArrayList<>();

    protected MealPlan() {
    }

    public MealPlan(UUID householdId, LocalDate weekStart) {
        this.id = UUID.randomUUID();
        this.householdId = householdId;
        this.weekStart = weekStart;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<MealPlanEntry> getEntries() {
        return entries;
    }

    public void addEntry(MealPlanEntry entry) {
        entry.setMealPlan(this);
        this.entries.add(entry);
    }

    public void removeEntry(MealPlanEntry entry) {
        this.entries.remove(entry);
        entry.setMealPlan(null);
    }
}
