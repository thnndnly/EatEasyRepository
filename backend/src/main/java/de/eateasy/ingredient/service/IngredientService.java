package de.eateasy.ingredient.service;

import de.eateasy.common.units.Unit;
import de.eateasy.ingredient.dto.IngredientDto;

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
}
