package de.eateasy.recipe.service;

import de.eateasy.recipe.dto.RecipeCreateRequest;
import de.eateasy.recipe.dto.RecipeDto;
import de.eateasy.recipe.dto.RecipeFilter;
import de.eateasy.recipe.dto.RecipeIngredientView;
import de.eateasy.recipe.dto.RecipeMiniDto;
import de.eateasy.recipe.dto.RecipeUpdateRequest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RecipeService {

    /** Liefert eigene + Haushalts-sichtbare Rezepte mit optionalen Filtern. */
    List<RecipeDto> list(UUID userId, RecipeFilter filter);

    /** Einzelnes Rezept; 403 wenn weder Owner noch Mitglied im verknuepften Haushalt. */
    RecipeDto get(UUID userId, UUID recipeId);

    /** Anlegen; bei {@code householdId != null} muss User Mitglied sein. */
    RecipeDto create(UUID userId, RecipeCreateRequest request);

    /** Komplett-Update; nur Owner. */
    RecipeDto update(UUID userId, UUID recipeId, RecipeUpdateRequest request);

    /** Loeschen; nur Owner. */
    void delete(UUID userId, UUID recipeId);

    /**
     * Batch-Lookup nur per ID — ohne Auth-Check. Wird von Komponenten genutzt,
     * die bereits einen authorisierten Container (z. B. einen MealPlan-Eintrag)
     * in der Hand haben und nur die Mini-Daten der referenzierten Rezepte
     * brauchen. Fehlende IDs tauchen schlicht nicht in der Map auf.
     */
    Map<UUID, RecipeMiniDto> getMinis(Collection<UUID> recipeIds);

    /**
     * Aggregations-Sicht: liefert pro {@code recipeId} die rohen Zutatenrows
     * (ingredientId, amount, unit) — ohne Auth-Check, ohne Names. Wird von
     * Einkaufslisten- und Smart-Suggestion-Pipelines genutzt.
     */
    Map<UUID, List<RecipeIngredientView>> getIngredientsByRecipeIds(Collection<UUID> recipeIds);
}
