package de.eateasy.household.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Patch-Request für einen Haushalt. Alle Felder sind optional —
 * nur gesetzte (non-null) Felder werden übernommen.
 */
public record HouseholdUpdateRequest(
    @Size(max = 100) String name,
    List<String> defaultDietTags,
    Boolean autoRestockEnabled
) {
}
