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
import de.eateasy.recipe.dto.RecipeMiniDto;
import de.eateasy.recipe.dto.RecipeUpdateRequest;
import de.eateasy.recipe.entity.Recipe;
import de.eateasy.recipe.entity.RecipeIngredient;
import de.eateasy.recipe.repository.RecipeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collection;
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
    private final IngredientService ingredientService;
    private final HouseholdService householdService;

    public RecipeServiceImpl(RecipeRepository recipeRepository,
                             IngredientService ingredientService,
                             HouseholdService householdService) {
        this.recipeRepository = recipeRepository;
        this.ingredientService = ingredientService;
        this.householdService = householdService;
    }

    @Override
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

        Map<UUID, IngredientDto> ingredientNames = loadIngredientNames(recipes);
        List<RecipeDto> result = new ArrayList<>(recipes.size());
        for (Recipe recipe : recipes) {
            result.add(toDto(recipe, ingredientNames));
        }
        return result;
    }

    @Override
    public RecipeDto get(UUID userId, UUID recipeId) {
        Recipe recipe = loadRecipe(recipeId);
        assertCanRead(userId, recipe);
        return toDto(recipe, loadIngredientNames(List.of(recipe)));
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

        return toDto(recipe, loadIngredientNames(List.of(recipe)));
    }

    @Override
    @Transactional
    public RecipeDto update(UUID userId, UUID recipeId, RecipeUpdateRequest request) {
        Recipe recipe = loadRecipe(recipeId);
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

        return toDto(recipe, loadIngredientNames(List.of(recipe)));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID recipeId) {
        Recipe recipe = loadRecipe(recipeId);
        assertOwner(userId, recipe);
        recipeRepository.delete(recipe);
    }

    @Override
    public Map<UUID, RecipeMiniDto> getMinis(Collection<UUID> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Map.of();
        }
        return recipeRepository.findByIds(recipeIds).stream()
            .map(RecipeMiniDto::from)
            .collect(Collectors.toMap(RecipeMiniDto::id, Function.identity()));
    }

    // --- Helpers ---------------------------------------------------------

    private Recipe loadRecipe(UUID recipeId) {
        return recipeRepository.findByIdOptional(recipeId)
            .orElseThrow(() -> new NotFoundException("Rezept nicht gefunden: " + recipeId));
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
            throw new ForbiddenException("Nur der Owner darf dieses Rezept aendern");
        }
    }

    private List<RecipeIngredient> buildIngredients(List<RecipeIngredientRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("Mindestens eine Zutat erforderlich");
        }
        List<RecipeIngredient> result = new ArrayList<>(requests.size());
        for (RecipeIngredientRequest req : requests) {
            UUID ingredientId = resolveIngredientId(req);
            result.add(new RecipeIngredient(ingredientId, req.amount(), req.unit(), req.note()));
        }
        return result;
    }

    private UUID resolveIngredientId(RecipeIngredientRequest req) {
        if (req.ingredientId() != null) {
            // Validate existence by triggering NotFound bei unbekannter ID.
            ingredientService.getById(req.ingredientId());
            return req.ingredientId();
        }
        if (req.ingredientName() == null || req.ingredientName().isBlank()) {
            throw new BadRequestException("ingredientId oder ingredientName muss gesetzt sein");
        }
        IngredientDto created = ingredientService.findOrCreate(req.ingredientName(), req.unit());
        return created.id();
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

    private static RecipeDto toDto(Recipe recipe, Map<UUID, IngredientDto> ingredientNames) {
        List<RecipeIngredientDto> ingredientDtos = new ArrayList<>(recipe.getIngredients().size());
        for (RecipeIngredient ri : recipe.getIngredients()) {
            IngredientDto ing = ingredientNames.get(ri.getIngredientId());
            String name = ing != null ? ing.name() : "(unbekannt)";
            ingredientDtos.add(RecipeIngredientDto.from(ri, name));
        }
        return RecipeDto.from(recipe, ingredientDtos);
    }
}
