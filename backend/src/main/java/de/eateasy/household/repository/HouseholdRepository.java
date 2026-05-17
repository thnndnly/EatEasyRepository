package de.eateasy.household.repository;

import de.eateasy.household.entity.Household;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class HouseholdRepository implements PanacheRepositoryBase<Household, UUID> {

    /**
     * Batch-Lookup mehrerer Haushalte in einem einzigen Query — verhindert
     * das N+1-Problem in {@code listForUser}.
     */
    public List<Household> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return list("id in ?1", ids);
    }
}
