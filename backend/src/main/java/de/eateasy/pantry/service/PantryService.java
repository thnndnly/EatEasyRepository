package de.eateasy.pantry.service;

import de.eateasy.pantry.dto.AddPantryItemRequest;
import de.eateasy.pantry.dto.PantryItemDto;
import de.eateasy.pantry.dto.UpdatePantryItemRequest;

import java.util.List;
import java.util.UUID;

public interface PantryService {

    /** Liefert alle Vorrats-Eintraege eines Haushalts, sortiert nach MHD. */
    List<PantryItemDto> list(UUID userId, UUID householdId);

    /**
     * Fuegt einen Eintrag hinzu. Existiert bereits ein Eintrag mit derselben
     * Zutat und Unit, werden die Mengen addiert (kein neuer Eintrag).
     * Bei abweichender Unit wird ein neuer Eintrag angelegt.
     */
    PantryItemDto add(UUID userId, UUID householdId, AddPantryItemRequest request);

    /** Aendert Menge, Unit oder MHD eines Eintrags. */
    PantryItemDto update(UUID userId, UUID itemId, UpdatePantryItemRequest request);

    /** Loescht einen Eintrag. */
    void delete(UUID userId, UUID itemId);
}
