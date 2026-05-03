package de.eateasy.pantry.repository;

import de.eateasy.common.units.Unit;
import de.eateasy.pantry.entity.PantryItem;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PantryItemRepository implements PanacheRepositoryBase<PantryItem, UUID> {

    /**
     * Liefert alle Vorrats-Eintraege eines Haushalts. Sortiert ASC nach
     * best_before, NULLs ans Ende — Postgres-Default ist NULLS LAST bei ASC.
     */
    public List<PantryItem> listByHousehold(UUID householdId) {
        // JPQL — entity-Feldnamen (camelCase), nicht SQL-Spalten.
        return list("householdId = ?1 order by bestBefore asc nulls last, createdAt asc",
            householdId);
    }

    /** Findet einen vorhandenen Slot mit derselben Zutat + Unit fuer Aggregation. */
    public Optional<PantryItem> findByHouseholdAndIngredientAndUnit(
        UUID householdId, UUID ingredientId, Unit unit) {
        return find("householdId = ?1 and ingredientId = ?2 and unit = ?3",
            householdId, ingredientId, unit).firstResultOptional();
    }
}
