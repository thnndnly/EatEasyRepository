package de.eateasy.integration.service;

import de.eateasy.common.exception.BadRequestException;
import de.eateasy.common.exception.NotFoundException;
import de.eateasy.integration.client.TheMealDbClient;
import de.eateasy.integration.client.TheMealDbResponse;
import de.eateasy.integration.client.TheMealDbResponse.TheMealDbMeal;
import de.eateasy.integration.dto.ExternalRecipePreviewDto;
import de.eateasy.integration.dto.RecipeImportRequest;
import de.eateasy.recipe.dto.RecipeCreateRequest;
import de.eateasy.recipe.dto.RecipeDto;
import de.eateasy.recipe.service.RecipeService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RecipeImportServiceImpl implements RecipeImportService {

    static final String SOURCE_THEMEALDB = "themealdb";

    private final TheMealDbClient theMealDbClient;
    private final RecipeService recipeService;

    public RecipeImportServiceImpl(@RestClient TheMealDbClient theMealDbClient,
                                   RecipeService recipeService) {
        this.theMealDbClient = theMealDbClient;
        this.recipeService = recipeService;
    }

    @Override
    public List<ExternalRecipePreviewDto> search(String source, String query) {
        if (!SOURCE_THEMEALDB.equalsIgnoreCase(source)) {
            throw new BadRequestException("Unbekannte Quelle: " + source);
        }
        TheMealDbResponse response = theMealDbClient.search(query == null ? "" : query);
        if (response == null || response.meals() == null) {
            return List.of();
        }
        List<ExternalRecipePreviewDto> result = new ArrayList<>();
        for (TheMealDbMeal meal : response.meals()) {
            result.add(new ExternalRecipePreviewDto(
                SOURCE_THEMEALDB,
                meal.idMeal(),
                meal.strMeal(),
                meal.strMealThumb(),
                meal.strCategory(),
                meal.strArea()));
        }
        return result;
    }

    @Override
    @Transactional
    public RecipeDto importRecipe(UUID userId, RecipeImportRequest request) {
        if (!SOURCE_THEMEALDB.equalsIgnoreCase(request.source())) {
            throw new BadRequestException("Unbekannte Quelle: " + request.source());
        }

        TheMealDbResponse response = theMealDbClient.lookup(request.externalId());
        if (response == null || response.meals() == null || response.meals().isEmpty()) {
            throw new NotFoundException(
                "Rezept nicht gefunden in " + request.source() + ": " + request.externalId());
        }
        TheMealDbMeal meal = response.meals().get(0);

        RecipeCreateRequest createRequest = TheMealDbMapper.toCreateRequest(meal, request.householdId());
        // RecipeService kapselt Auth-Check fuer householdId, Diaet-Tag-Validation
        // und Ingredient-findOrCreate. Wir muessen anschliessend nur noch die
        // externen Felder nachpflegen.
        RecipeDto created = recipeService.create(userId, createRequest);

        // Externe Metadaten nachpflegen via RecipeService — wir vermeiden bewusst
        // den direkten Zugriff auf RecipeRepository ueber Komponentengrenzen hinweg.
        recipeService.updateExternalMetadata(created.id(), safeSourceUrl(meal), SOURCE_THEMEALDB);

        // Re-fetch ueber RecipeService, damit der Aufrufer die aktualisierten
        // Felder im DTO sieht (sourceUrl, externalSource).
        return recipeService.get(userId, created.id());
    }

    private static String safeSourceUrl(TheMealDbMeal meal) {
        if (meal.strSource() != null && !meal.strSource().isBlank()) {
            return meal.strSource();
        }
        // Fallback: TheMealDB hat fuer jedes Rezept eine eigene Detail-URL.
        return "https://www.themealdb.com/meal/" + meal.idMeal();
    }
}
