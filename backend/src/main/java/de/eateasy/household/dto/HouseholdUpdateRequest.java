package de.eateasy.household.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Patch-Request fuer einen Haushalt. Beide Felder sind optional —
 * nur gesetzte Felder werden uebernommen.
 */
public record HouseholdUpdateRequest(
    @Size(max = 100) String name,
    List<String> defaultDietTags
) {
}
