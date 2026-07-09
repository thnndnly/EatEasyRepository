package de.eateasy.integration.service;

import de.eateasy.common.exception.NotFoundException;
import de.eateasy.integration.client.OpenFoodFactsClient;
import de.eateasy.integration.client.OpenFoodFactsResponse;
import de.eateasy.integration.dto.BarcodePantryRequest;
import de.eateasy.integration.dto.BarcodeProductDto;
import de.eateasy.pantry.dto.AddPantryItemRequest;
import de.eateasy.pantry.dto.PantryItemDto;
import de.eateasy.pantry.service.PantryService;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.UUID;

@ApplicationScoped
public class BarcodeServiceImpl implements BarcodeService {

    private final OpenFoodFactsClient openFoodFactsClient;
    private final PantryService pantryService;

    public BarcodeServiceImpl(@RestClient OpenFoodFactsClient openFoodFactsClient,
                              PantryService pantryService) {
        this.openFoodFactsClient = openFoodFactsClient;
        this.pantryService = pantryService;
    }

    @Override
    public BarcodeProductDto lookup(String barcode) {
        OpenFoodFactsResponse response = openFoodFactsClient.getProduct(barcode);
        BarcodeProductDto dto = OpenFoodFactsMapper.toDto(barcode, response);
        if (dto == null) {
            throw new NotFoundException("Kein Produkt für Barcode gefunden: " + barcode);
        }
        return dto;
    }

    @Override
    public PantryItemDto addToPantry(UUID userId, UUID householdId, BarcodePantryRequest request) {
        BarcodeProductDto product = lookup(request.barcode());
        AddPantryItemRequest pantryRequest = new AddPantryItemRequest(
            null,
            product.name(),
            request.amount(),
            request.unit(),
            request.bestBefore());
        return pantryService.add(userId, householdId, pantryRequest);
    }
}
