package de.eateasy.pantry.service;

import de.eateasy.common.exception.ForbiddenException;
import de.eateasy.common.exception.NotFoundException;
import de.eateasy.common.units.Unit;
import de.eateasy.household.service.HouseholdService;
import de.eateasy.ingredient.dto.IngredientDto;
import de.eateasy.ingredient.service.IngredientService;
import de.eateasy.pantry.dto.AddPantryItemRequest;
import de.eateasy.pantry.dto.PantryItemDto;
import de.eateasy.pantry.dto.UpdatePantryItemRequest;
import de.eateasy.pantry.entity.PantryItem;
import de.eateasy.pantry.repository.PantryItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class PantryServiceImpl implements PantryService {

    private final PantryItemRepository pantryRepository;
    private final HouseholdService householdService;
    private final IngredientService ingredientService;

    public PantryServiceImpl(PantryItemRepository pantryRepository,
                             HouseholdService householdService,
                             IngredientService ingredientService) {
        this.pantryRepository = pantryRepository;
        this.householdService = householdService;
        this.ingredientService = ingredientService;
    }

    @Override
    @Transactional
    public List<PantryItemDto> list(UUID userId, UUID householdId) {
        householdService.assertMember(userId, householdId);
        List<PantryItem> items = pantryRepository.listByHousehold(householdId);
        Map<UUID, IngredientDto> names = collectIngredientNames(items);
        List<PantryItemDto> result = new ArrayList<>(items.size());
        for (PantryItem item : items) {
            result.add(toDto(item, names));
        }
        return result;
    }

    @Override
    @Transactional
    public PantryItemDto add(UUID userId, UUID householdId, AddPantryItemRequest request) {
        householdService.assertMember(userId, householdId);

        UUID ingredientId = ingredientService.resolveOrCreate(
            request.ingredientId(), request.ingredientName(), request.unit());

        Optional<PantryItem> existing = pantryRepository
            .findByHouseholdAndIngredientAndUnit(householdId, ingredientId, request.unit());

        PantryItem item;
        if (existing.isPresent()) {
            item = existing.get();
            item.setAmount(item.getAmount().add(request.amount()));
            // MHD wird nur ueberschrieben, wenn neuer Eintrag eines hat und der
            // bestehende keinen — sonst behaelt der bestehende sein MHD.
            if (item.getBestBefore() == null && request.bestBefore() != null) {
                item.setBestBefore(request.bestBefore());
            }
        } else {
            item = new PantryItem(
                householdId,
                ingredientId,
                request.amount(),
                request.unit(),
                request.bestBefore());
            pantryRepository.persist(item);
        }

        IngredientDto ingredient = ingredientService.getById(ingredientId);
        return PantryItemDto.from(item, ingredient.name());
    }

    @Override
    @Transactional
    public PantryItemDto update(UUID userId, UUID itemId, UpdatePantryItemRequest request) {
        PantryItem item = loadItem(itemId);
        if (!householdService.isMember(userId, item.getHouseholdId())) {
            throw new ForbiddenException("Kein Zugriff auf diesen Vorrats-Eintrag");
        }

        if (request.amount() != null) {
            item.setAmount(request.amount());
        }
        if (request.unit() != null) {
            item.setUnit(request.unit());
        }
        if (request.bestBefore() != null) {
            item.setBestBefore(request.bestBefore());
        }

        IngredientDto ingredient = ingredientService.getById(item.getIngredientId());
        return PantryItemDto.from(item, ingredient.name());
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID itemId) {
        PantryItem item = loadItem(itemId);
        if (!householdService.isMember(userId, item.getHouseholdId())) {
            throw new ForbiddenException("Kein Zugriff auf diesen Vorrats-Eintrag");
        }
        pantryRepository.delete(item);
    }

    /**
     * Liefert den aggregierten Vorrat eines Haushalts. Bewusst OHNE eigenen
     * Auth-Check und ohne {@code userId} — der Aufrufer (z. B.
     * {@code ShoppingListService}/{@code SmartSuggestionService}) hat den
     * Haushalts-Zugriff bereits geprueft, analog zu
     * {@code HouseholdService.isAutoRestockEnabled}.
     */
    @Override
    public Map<UUID, Map<Unit, BigDecimal>> getInventory(UUID householdId) {
        Map<UUID, Map<Unit, BigDecimal>> result = new HashMap<>();
        for (PantryItem item : pantryRepository.listByHousehold(householdId)) {
            result.computeIfAbsent(item.getIngredientId(), k -> new EnumMap<>(Unit.class))
                .merge(item.getUnit(), item.getAmount(), BigDecimal::add);
        }
        return result;
    }

    // --- Helpers ---------------------------------------------------------

    private PantryItem loadItem(UUID itemId) {
        return pantryRepository.findByIdOptional(itemId)
            .orElseThrow(() -> new NotFoundException("Vorrats-Eintrag nicht gefunden: " + itemId));
    }

    private Map<UUID, IngredientDto> collectIngredientNames(List<PantryItem> items) {
        Set<UUID> ids = new HashSet<>();
        for (PantryItem item : items) {
            ids.add(item.getIngredientId());
        }
        return ingredientService.getByIds(ids);
    }

    private static PantryItemDto toDto(PantryItem item, Map<UUID, IngredientDto> names) {
        IngredientDto ingredient = names.get(item.getIngredientId());
        String name = ingredient != null ? ingredient.name() : "(unbekannt)";
        return PantryItemDto.from(item, name);
    }
}
