package de.eateasy.household.repository;

import de.eateasy.household.entity.HouseholdMembership;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class HouseholdMembershipRepository implements PanacheRepositoryBase<HouseholdMembership, UUID> {

    public Optional<HouseholdMembership> findByUserAndHousehold(UUID userId, UUID householdId) {
        return find("userId = ?1 and householdId = ?2", userId, householdId).firstResultOptional();
    }

    public List<HouseholdMembership> findByUser(UUID userId) {
        return list("userId", userId);
    }

    public List<HouseholdMembership> findByHousehold(UUID householdId) {
        return list("householdId", householdId);
    }

    public boolean existsByUserAndHousehold(UUID userId, UUID householdId) {
        return count("userId = ?1 and householdId = ?2", userId, householdId) > 0;
    }
}
