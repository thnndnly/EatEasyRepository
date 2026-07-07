package de.eateasy.household.dto;

import de.eateasy.household.entity.Household;
import de.eateasy.household.entity.MembershipRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sicht auf einen Haushalt fuer den eingeloggten User. {@code role} ist die
 * Rolle dieses Users im Haushalt — der Wert entscheidet ueber Edit-Rechte im
 * Frontend.
 */
public record HouseholdDto(
    UUID id,
    String name,
    List<String> defaultDietTags,
    boolean autoRestockEnabled,
    MembershipRole role,
    Instant createdAt
) {
    public static HouseholdDto from(Household household, MembershipRole role) {
        return new HouseholdDto(
            household.getId(),
            household.getName(),
            List.of(household.getDefaultDietTags()),
            household.isAutoRestockEnabled(),
            role,
            household.getCreatedAt());
    }
}
