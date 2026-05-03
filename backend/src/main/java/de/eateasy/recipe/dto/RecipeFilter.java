package de.eateasy.recipe.dto;

import java.util.List;
import java.util.UUID;

/**
 * Query-Parameter fuer GET /recipes. Wird von der Resource zusammengebaut und
 * an den Service weitergereicht. Alle Felder sind optional.
 */
public record RecipeFilter(
    String query,
    List<String> dietTags,
    UUID householdId
) {
}
