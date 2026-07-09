package de.eateasy.pantry.service;

import de.eateasy.common.units.Unit;
import de.eateasy.pantry.dto.AddPantryItemRequest;
import de.eateasy.pantry.dto.PantryItemDto;
import de.eateasy.pantry.dto.UpdatePantryItemRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PantryService {

    /** Liefert alle Vorrats-Einträge eines Haushalts, sortiert nach MHD. */
    List<PantryItemDto> list(UUID userId, UUID householdId);

    /**
     * Fügt einen Eintrag hinzu. Existiert bereits ein Eintrag mit derselben
     * Zutat und Unit, werden die Mengen addiert (kein neuer Eintrag).
     * Bei abweichender Unit wird ein neuer Eintrag angelegt.
     */
    PantryItemDto add(UUID userId, UUID householdId, AddPantryItemRequest request);

    /** Ändert Menge, Unit oder MHD eines Eintrags. */
    PantryItemDto update(UUID userId, UUID itemId, UpdatePantryItemRequest request);

    /** Löscht einen Eintrag. */
    void delete(UUID userId, UUID itemId);

    /**
     * Aggregiertes Inventar: pro Zutat × Unit-Kombination die summierte Menge
     * im Vorrat. Ohne Auth-Check — wird von der ShoppingList-Pipeline genutzt,
     * die den Aufruf-Kontext selbst absichert.
     */
    Map<UUID, Map<Unit, BigDecimal>> getInventory(UUID householdId);
}
