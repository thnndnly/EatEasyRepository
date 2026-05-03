package de.eateasy.mealplan.repository;

import de.eateasy.mealplan.entity.MealPlan;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MealPlanRepository implements PanacheRepositoryBase<MealPlan, UUID> {

    public Optional<MealPlan> findByHouseholdAndWeek(UUID householdId, LocalDate weekStart) {
        return find("householdId = ?1 and weekStart = ?2", householdId, weekStart)
            .firstResultOptional();
    }
}
