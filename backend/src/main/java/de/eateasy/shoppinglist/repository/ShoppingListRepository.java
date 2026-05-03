package de.eateasy.shoppinglist.repository;

import de.eateasy.shoppinglist.entity.ShoppingList;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ShoppingListRepository implements PanacheRepositoryBase<ShoppingList, UUID> {

    public Optional<ShoppingList> findByMealPlan(UUID mealPlanId) {
        return find("mealPlanId", mealPlanId).firstResultOptional();
    }
}
