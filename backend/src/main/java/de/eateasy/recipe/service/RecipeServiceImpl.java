package de.eateasy.recipe.service;

import de.eateasy.common.diet.DietTag;
import de.eateasy.common.exception.BadRequestException;
import de.eateasy.common.exception.ForbiddenException;
import de.eateasy.common.exception.NotFoundException;
import de.eateasy.household.service.HouseholdService;
import de.eateasy.ingredient.dto.IngredientDto;
import de.eateasy.ingredient.service.IngredientService;
import de.eateasy.recipe.dto.RecipeCreateRequest;
import de.eateasy.recipe.dto.RecipeDto;
import de.eateasy.recipe.dto.RecipeFilter;
import de.eateasy.recipe.dto.RecipeIngredientDto;
import de.eateasy.recipe.dto.RecipeIngredientRequest;
import de.eateasy.recipe.dto.RecipeIngredientView;
import de.eateasy.recipe.dto.RecipeMiniDto;
import de.eateasy.recipe.dto.RecipeUpdateRequest;
import de.eateasy.recipe.entity.Recipe;
import de.eateasy.recipe.entity.RecipeIngredient;
import de.eateasy.recipe.repository.RecipeFavoriteRepository;
import de.eateasy.recipe.repository.RecipeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeFavoriteRepository favoriteRepository;
    private final IngredientService ingredientService;
    private final HouseholdService householdService;

    public RecipeServiceImpl(RecipeRepository recipeRepository,
                             RecipeFavoriteRepository favoriteRepository,
                             IngredientService ingredientService,
                             HouseholdService householdService) {
        this.recipeRepository = recipeRepository;
        this.favoriteRepository = favoriteRepository;
        this.ingredientService = ingredientService;
        this.householdService = householdService;
    }

    @Override
    @Transactional
    public List<RecipeDto> list(UUID userId, RecipeFilter filter) {
        List<UUID> householdIds = householdService.listHouseholdIdsForUser(userId);

        if (filter.householdId() != null && !householdIds.contains(filter.householdId())) {
            throw new ForbiddenException("Kein Zugriff auf diesen Haushalt");
        }

        List<Recipe> recipes = recipeRepository.search(
            userId,
            householdIds,
            filter.query(),
            filter.dietTags(),
            filter.householdId());

        Set<UUID> favoriteIds = favoriteRepository.findRecipeIdsByUser(userId);
        if (filter.favoritesOnly()) {
            recipes = recipes.stream().filter(r -> favoriteIds.contains(r.getId())).toList();
        }

        Map<UUID, IngredientDto> ingredientNames = loadIngredientNames(recipes);
        List<RecipeDto> result = new ArrayList<>(recipes.size());
        for (Recipe recipe : recipes) {
            result.add(toDto(recipe, ingredientNames, favoriteIds.contains(recipe.getId())));
        }
        return result;
    }

    @Override
    @Transactional
    public RecipeDto get(UUID userId, UUID recipeId) {
        Recipe recipe = loadActiveRecipe(recipeId);
        assertCanRead(userId, recipe);
        boolean favorite = favoriteRepository.findByUserAndRecipe(userId, recipeId).isPresent();
        return toDto(recipe, loadIngredientNames(List.of(recipe)), favorite);
    }

    @Override
    @Transactional
    public RecipeDto create(UUID userId, RecipeCreateRequest request) {
        if (request.householdId() != null && !householdService.isMember(userId, request.householdId())) {
            throw new ForbiddenException("Du bist kein Mitglied dieses Haushalts");
        }
        String[] tags = DietTag.validate(request.dietTags());

        Recipe recipe = new Recipe(
            userId,
            request.householdId(),
            request.title().trim(),
            request.description(),
            request.instructions(),
            request.servings(),
            request.prepMinutes(),
            tags,
            null,
            null);

        recipe.replaceIngredients(buildIngredients(request.ingredients()));
        recipeRepository.persist(recipe);

        // Frisch angelegt — kann noch kein Favorit sein.
        return toDto(recipe, loadIngredientNames(List.of(recipe)), false);
    }

    @Override
    @Transactional
    public RecipeDto update(UUID userId, UUID recipeId, RecipeUpdateRequest request) {
        Recipe recipe = loadActiveRecipe(recipeId);
        assertOwner(userId, recipe);

        if (request.householdId() != null && !householdService.isMember(userId, request.householdId())) {
            throw new ForbiddenException("Du bist kein Mitglied dieses Haushalts");
        }
        String[] tags = DietTag.validate(request.dietTags());

        recipe.setTitle(request.title().trim());
        recipe.setDescription(request.description());
        recipe.setInstructions(request.instructions());
        recipe.setServings(request.servings());
        recipe.setPrepMinutes(request.prepMinutes());
        recipe.setDietTags(tags);
        recipe.setHouseholdId(request.householdId());
        recipe.replaceIngredients(buildIngredients(request.ingredients()));

        boolean favorite = favoriteRepository.findByUserAndRecipe(userId, recipeId).isPresent();
        return toDto(recipe, loadIngredientNames(List.of(recipe)), favorite);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID recipeId) {
        Recipe recipe = loadActiveRecipe(recipeId);
        assertOwner(userId, recipe);
        // Soft-Delete: als gelöscht markieren statt entfernen, damit bestehende
        // Wochenplan-/Einkaufslisten-/Favoriten-Referenzen erhalten bleiben.
        recipe.markDeleted();
    }

    @Override
    @Transactional
    public void setFavorite(UUID userId, UUID recipeId, boolean favorite) {
        Recipe recipe = loadActiveRecipe(recipeId);
        assertCanRead(userId, recipe);

        if (favorite) {
            // Idempotenter Upsert (ON CONFLICT DO NOTHING) statt check-then-act:
            // atomar auf DB-Ebene, damit parallele Requests keinen Unique-Constraint-500 auslösen.
            favoriteRepository.insertIfAbsent(userId, recipeId);
        } else {
            favoriteRepository.findByUserAndRecipe(userId, recipeId)
                .ifPresent(favoriteRepository::delete);
        }
        // Gewünschter Zustand besteht danach in jedem Fall — idempotent, kein Fehler.
    }

    @Override
    @Transactional
    public void updateExternalMetadata(UUID recipeId, String sourceUrl, String externalSource) {
        Recipe recipe = loadRecipe(recipeId);
        recipe.setSourceUrl(sourceUrl);
        recipe.setExternalSource(externalSource);
    }

    @Override
    @Transactional
    public Map<UUID, RecipeMiniDto> getMinis(Collection<UUID> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Map.of();
        }
        return recipeRepository.findByIds(recipeIds).stream()
            .map(RecipeMiniDto::from)
            .collect(Collectors.toMap(RecipeMiniDto::id, Function.identity()));
    }

    @Override
    @Transactional
    public Map<UUID, List<RecipeIngredientView>> getIngredientsByRecipeIds(Collection<UUID> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<RecipeIngredientView>> result = new HashMap<>();
        for (Recipe recipe : recipeRepository.findByIds(recipeIds)) {
            List<RecipeIngredientView> rows = new ArrayList<>(recipe.getIngredients().size());
            for (RecipeIngredient ri : recipe.getIngredients()) {
                rows.add(new RecipeIngredientView(ri.getIngredientId(), ri.getAmount(), ri.getUnit()));
            }
            result.put(recipe.getId(), rows);
        }
        return result;
    }

    // --- Helpers ---------------------------------------------------------

    private Recipe loadRecipe(UUID recipeId) {
        return recipeRepository.findByIdOptional(recipeId)
            .orElseThrow(() -> new NotFoundException("Rezept nicht gefunden: " + recipeId));
    }

    /**
     * Wie {@link #loadRecipe}, behandelt soft-gelöschte Rezepte aber als nicht
     * vorhanden (404) — für Lese-/Schreibpfade, die nur aktive Rezepte sehen
     * dürfen. Die Referenz-Auflösung ({@code getMinis}/
     * {@code getIngredientsByRecipeIds}) nutzt bewusst {@code findByIds} ohne
     * diesen Filter, damit bestehende Wochenplan-/Einkaufslisten-Einträge
     * gelöschte Rezepte weiterhin darstellen können.
     */
    private Recipe loadActiveRecipe(UUID recipeId) {
        Recipe recipe = loadRecipe(recipeId);
        if (recipe.isDeleted()) {
            throw new NotFoundException("Rezept nicht gefunden: " + recipeId);
        }
        return recipe;
    }

    private void assertCanRead(UUID userId, Recipe recipe) {
        if (recipe.getOwnerId().equals(userId)) {
            return;
        }
        if (recipe.getHouseholdId() != null
            && householdService.isMember(userId, recipe.getHouseholdId())) {
            return;
        }
        throw new ForbiddenException("Kein Zugriff auf dieses Rezept");
    }

    private void assertOwner(UUID userId, Recipe recipe) {
        if (!recipe.getOwnerId().equals(userId)) {
            throw new ForbiddenException("Nur der Owner darf dieses Rezept ändern");
        }
    }

    private List<RecipeIngredient> buildIngredients(List<RecipeIngredientRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("Mindestens eine Zutat erforderlich");
        }
        List<RecipeIngredient> result = new ArrayList<>(requests.size());
        for (RecipeIngredientRequest req : requests) {
            UUID ingredientId = ingredientService.resolveOrCreate(
                req.ingredientId(), req.ingredientName(), req.unit());
            result.add(new RecipeIngredient(ingredientId, req.amount(), req.unit(), req.note()));
        }
        return result;
    }

    private Map<UUID, IngredientDto> loadIngredientNames(List<Recipe> recipes) {
        Set<UUID> ids = new HashSet<>();
        for (Recipe r : recipes) {
            for (RecipeIngredient ri : r.getIngredients()) {
                ids.add(ri.getIngredientId());
            }
        }
        return ingredientService.getByIds(ids);
    }

    private static RecipeDto toDto(Recipe recipe, Map<UUID, IngredientDto> ingredientNames,
                                   boolean favorite) {
        List<RecipeIngredientDto> ingredientDtos = new ArrayList<>(recipe.getIngredients().size());
        for (RecipeIngredient ri : recipe.getIngredients()) {
            IngredientDto ing = ingredientNames.get(ri.getIngredientId());
            String name = ing != null ? ing.name() : "(unbekannt)";
            ingredientDtos.add(RecipeIngredientDto.from(ri, name));
        }
        return RecipeDto.from(recipe, ingredientDtos, favorite);
    }
}
