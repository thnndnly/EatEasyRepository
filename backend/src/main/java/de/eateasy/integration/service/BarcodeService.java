package de.eateasy.integration.service;

import de.eateasy.integration.dto.BarcodePantryRequest;
import de.eateasy.integration.dto.BarcodeProductDto;
import de.eateasy.pantry.dto.PantryItemDto;

import java.util.UUID;

public interface BarcodeService {

    /**
     * Schlägt einen Barcode bei OpenFoodFacts nach und liefert das Preview.
     * Wirft {@code NotFoundException}, wenn das Produkt unbekannt ist.
     */
    BarcodeProductDto lookup(String barcode);

    /**
     * Fügt das Produkt in den Vorrat eines Haushalts ein. Legt die Zutat
     * automatisch an, wenn sie noch nicht existiert. Auth-Check erfolgt im
     * darunterliegenden {@code PantryService}.
     */
    PantryItemDto addToPantry(UUID userId, UUID householdId, BarcodePantryRequest request);
}
