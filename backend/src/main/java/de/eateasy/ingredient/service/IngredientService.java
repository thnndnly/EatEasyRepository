package de.eateasy.ingredient.service;

import de.eateasy.common.units.Unit;
import de.eateasy.ingredient.dto.IngredientDto;
import de.eateasy.ingredient.entity.IngredientCategory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IngredientService {

    /**
     * Idempotente Anlage: existiert eine Zutat mit demselben Namen
     * (case-insensitive), wird die bestehende zurueckgegeben — sonst neu
     * angelegt. Wird von Recipe-Service waehrend Anlage/Edit aufgerufen.
     */
    IngredientDto findOrCreate(String name, Unit defaultUnit);

    /** Suchergebnis fuer Autocomplete-Picker im Frontend. */
    List<IngredientDto> search(String query, int limit);

    IngredientDto getById(UUID id);

    /**
     * Batch-Lookup. Wird vom RecipeService genutzt, um die Namen aller
     * Recipe-Zutaten in einer Query zu laden statt n+1.
     */
    Map<UUID, IngredientDto> getByIds(Collection<UUID> ids);

    /**
     * Setzt die Supermarkt-Kategorie einer Zutat (Phase 16). Zutaten sind ein
     * global geteilter Pool — die Kategorie gilt damit fuer alle Haushalte.
     */
    IngredientDto updateCategory(UUID id, IngredientCategory category);

    /**
     * Loest eine Zutat anhand von (optionaler) ID oder Name auf. Ist {@code id}
     * gesetzt, wird die Existenz validiert und die ID zurueckgegeben — sonst
     * wird per {@link #findOrCreate(String, Unit)} angelegt. Vereinheitlicht
     * das Pattern, das sonst in {@code RecipeServiceImpl} und
     * {@code PantryServiceImpl} dupliziert ist.
     */
    UUID resolveOrCreate(UUID id, String name, Unit defaultUnit);
}
