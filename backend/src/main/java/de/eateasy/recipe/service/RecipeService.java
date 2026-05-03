package de.eateasy.recipe.service;

import de.eateasy.recipe.dto.RecipeCreateRequest;
import de.eateasy.recipe.dto.RecipeDto;
import de.eateasy.recipe.dto.RecipeFilter;
import de.eateasy.recipe.dto.RecipeUpdateRequest;

import java.util.List;
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
}
