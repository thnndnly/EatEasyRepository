package de.eateasy.integration.service;

import de.eateasy.integration.dto.ExternalRecipePreviewDto;
import de.eateasy.integration.dto.RecipeImportRequest;
import de.eateasy.recipe.dto.RecipeDto;

import java.util.List;
import java.util.UUID;

public interface RecipeImportService {

    /** Suche in einer externen Quelle. Aktuell nur {@code source = "themealdb"}. */
    List<ExternalRecipePreviewDto> search(String source, String query);

    /**
     * Holt ein externes Rezept anhand seiner ID, mappt es auf unsere Struktur
     * und legt es als ganz normales {@code Recipe} fuer den User an.
     */
    RecipeDto importRecipe(UUID userId, RecipeImportRequest request);
}
