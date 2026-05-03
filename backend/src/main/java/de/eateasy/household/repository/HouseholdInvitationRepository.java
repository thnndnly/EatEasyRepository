package de.eateasy.household.repository;

import de.eateasy.household.entity.HouseholdInvitation;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class HouseholdInvitationRepository implements PanacheRepositoryBase<HouseholdInvitation, UUID> {

    public Optional<HouseholdInvitation> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }
}
