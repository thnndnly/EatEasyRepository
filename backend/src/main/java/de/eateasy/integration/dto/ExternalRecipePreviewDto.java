package de.eateasy.integration.dto;

/**
 * Such-Treffer aus einer externen Quelle. Wird im Frontend in der
 * Import-Vorschau angezeigt; vom User mit "Importieren" zur vollen Anlage
 * via {@code POST /recipes/import} bestaetigt.
 */
public record ExternalRecipePreviewDto(
    String source,
    String externalId,
    String title,
    String thumbnailUrl,
    String category,
    String area
) {
}
