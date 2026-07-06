package de.eateasy.shoppinglist.service;

import de.eateasy.common.exception.NotFoundException;
import de.eateasy.common.units.Unit;
import de.eateasy.common.units.UnitConverter;
import de.eateasy.household.service.HouseholdService;
import de.eateasy.ingredient.dto.IngredientDto;
import de.eateasy.ingredient.entity.IngredientCategory;
import de.eateasy.ingredient.service.IngredientService;
import de.eateasy.mealplan.dto.MealPlanDto;
import de.eateasy.mealplan.dto.MealPlanEntryDto;
import de.eateasy.mealplan.service.MealPlanService;
import de.eateasy.pantry.dto.AddPantryItemRequest;
import de.eateasy.pantry.service.PantryService;
import de.eateasy.recipe.dto.RecipeIngredientView;
import de.eateasy.recipe.dto.RecipeMiniDto;
import de.eateasy.recipe.service.RecipeService;
import de.eateasy.shoppinglist.dto.ShoppingListDto;
import de.eateasy.shoppinglist.dto.ShoppingListItemDto;
import de.eateasy.shoppinglist.entity.ShoppingList;
import de.eateasy.shoppinglist.entity.ShoppingListItem;
import de.eateasy.shoppinglist.repository.ShoppingListItemRepository;
import de.eateasy.shoppinglist.repository.ShoppingListRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ShoppingListServiceImpl implements ShoppingListService {

    /** Schluessel fuer die Aggregation: gleiche Zutat in gleicher Einheit. */
    private record Key(UUID ingredientId, Unit unit) {
    }

    private static final int SCALING_PRECISION = 4;
    private static final int AMOUNT_SCALE = 2;

    private final ShoppingListRepository listRepository;
    private final ShoppingListItemRepository itemRepository;
    private final MealPlanService mealPlanService;
    private final RecipeService recipeService;
    private final PantryService pantryService;
    private final IngredientService ingredientService;
    private final HouseholdService householdService;

    public ShoppingListServiceImpl(ShoppingListRepository listRepository,
                                   ShoppingListItemRepository itemRepository,
                                   MealPlanService mealPlanService,
                                   RecipeService recipeService,
                                   PantryService pantryService,
                                   IngredientService ingredientService,
                                   HouseholdService householdService) {
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.mealPlanService = mealPlanService;
        this.recipeService = recipeService;
        this.pantryService = pantryService;
        this.ingredientService = ingredientService;
        this.householdService = householdService;
    }

    @Override
    @Transactional
    public ShoppingListDto getOrGenerate(UUID userId, UUID mealPlanId) {
        // Auth-Check ueber MealPlan: getById wirft 403 wenn der User kein
        // Mitglied des zugehoerigen Haushalts ist.
        MealPlanDto plan = mealPlanService.getById(userId, mealPlanId);

        return listRepository.findByMealPlan(mealPlanId)
            .map(this::toDto)
            .orElseGet(() -> regenerateInternal(plan));
    }

    @Override
    @Transactional
    public ShoppingListDto regenerate(UUID userId, UUID mealPlanId) {
        MealPlanDto plan = mealPlanService.getById(userId, mealPlanId);
        return regenerateInternal(plan);
    }

    @Override
    @Transactional
    public ShoppingListItemDto toggleChecked(UUID userId, UUID itemId, boolean checked) {
        ShoppingListItem item = itemRepository.findByIdOptional(itemId)
            .orElseThrow(() -> new NotFoundException("Listen-Eintrag nicht gefunden: " + itemId));

        UUID householdId = item.getShoppingList().getHouseholdId();
        householdService.assertMember(userId, householdId);
        boolean wasChecked = item.isChecked();
        item.setChecked(checked);

        // Auto-Nachbuchen: false→true bedeutet "gekauft" → landet im Vorrat.
        // Mehrfaches Toggeln wuerde sonst Duplikate erzeugen, daher nur beim
        // echten Uebergang. PantryService.add aggregiert ohnehin gleiche
        // (Zutat, Unit)-Eintraege, also doppelt-aufrufen waere nur unschoen,
        // nicht falsch — wir vermeiden es trotzdem.
        if (checked && !wasChecked) {
            pantryService.add(userId, householdId, new AddPantryItemRequest(
                item.getIngredientId(),
                null,
                item.getAmount(),
                item.getUnit(),
                null));
        }

        IngredientDto ingredient = ingredientService.getById(item.getIngredientId());
        return ShoppingListItemDto.from(item, ingredient.name(), ingredient.category());
    }

    // --- Berechnung -----------------------------------------------------

    private ShoppingListDto regenerateInternal(MealPlanDto plan) {
        Map<Key, BigDecimal> needed = aggregateNeeded(plan);
        Map<Key, BigDecimal> diff = subtractPantry(needed, plan.householdId());

        ShoppingList list = listRepository.findByMealPlan(plan.id()).orElse(null);
        Map<Key, Boolean> oldChecked = new HashMap<>();
        if (list != null) {
            for (ShoppingListItem old : list.getItems()) {
                oldChecked.put(new Key(old.getIngredientId(), old.getUnit()), old.isChecked());
            }
            list.clearItems();
        } else {
            list = new ShoppingList(plan.householdId(), plan.id());
            listRepository.persist(list);
        }

        for (Map.Entry<Key, BigDecimal> entry : diff.entrySet()) {
            boolean wasChecked = oldChecked.getOrDefault(entry.getKey(), false);
            ShoppingListItem item = new ShoppingListItem(
                entry.getKey().ingredientId(),
                entry.getValue(),
                entry.getKey().unit(),
                wasChecked);
            list.addItem(item);
        }
        // Damit eingebettete Items eine ID haben.
        itemRepository.flush();

        return toDto(list);
    }

    private Map<Key, BigDecimal> aggregateNeeded(MealPlanDto plan) {
        Set<UUID> recipeIds = new HashSet<>();
        Map<UUID, RecipeMiniDto> recipeMinis = collectRecipeMinis(plan, recipeIds);
        Map<UUID, List<RecipeIngredientView>> recipeIngredients =
            recipeService.getIngredientsByRecipeIds(recipeIds);

        Map<Key, BigDecimal> needed = new HashMap<>();
        for (MealPlanEntryDto entry : plan.entries()) {
            UUID recipeId = entry.recipe() != null ? entry.recipe().id() : null;
            if (recipeId == null) {
                continue;
            }
            RecipeMiniDto mini = recipeMinis.get(recipeId);
            List<RecipeIngredientView> ingredients = recipeIngredients.get(recipeId);
            if (mini == null || ingredients == null || mini.servings() <= 0) {
                continue;
            }
            BigDecimal scaling = BigDecimal.valueOf(entry.servings())
                .divide(BigDecimal.valueOf(mini.servings()), SCALING_PRECISION, RoundingMode.HALF_UP);
            for (RecipeIngredientView ingredient : ingredients) {
                BigDecimal scaled = ingredient.amount().multiply(scaling);
                // Normalisieren auf kanonische Einheit, damit gleiche Zutat in
                // unterschiedlichen Einheiten (z. B. TBSP vs ML) zusammenfaellt.
                Unit canonicalUnit = UnitConverter.canonical(ingredient.unit());
                BigDecimal canonicalAmount = UnitConverter.toCanonical(scaled, ingredient.unit());
                needed.merge(new Key(ingredient.ingredientId(), canonicalUnit),
                    canonicalAmount, BigDecimal::add);
            }
        }
        return needed;
    }

    private Map<UUID, RecipeMiniDto> collectRecipeMinis(MealPlanDto plan, Set<UUID> idsOut) {
        for (MealPlanEntryDto entry : plan.entries()) {
            if (entry.recipe() != null) {
                idsOut.add(entry.recipe().id());
            }
        }
        return recipeService.getMinis(idsOut);
    }

    private Map<Key, BigDecimal> subtractPantry(Map<Key, BigDecimal> needed, UUID householdId) {
        Map<UUID, Map<Unit, BigDecimal>> rawInventory = pantryService.getInventory(householdId);
        // Auch das Inventar in kanonische Einheiten normalisieren — sonst zieht
        // ein Pantry-Eintrag in TBSP nichts von einer ML-needs-Zeile ab.
        Map<UUID, Map<Unit, BigDecimal>> inventory = canonicalizeInventory(rawInventory);

        Map<Key, BigDecimal> diff = new HashMap<>();
        for (Map.Entry<Key, BigDecimal> entry : needed.entrySet()) {
            BigDecimal pantry = inventory.getOrDefault(entry.getKey().ingredientId(), Map.of())
                .getOrDefault(entry.getKey().unit(), BigDecimal.ZERO);
            BigDecimal remaining = entry.getValue().subtract(pantry);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                diff.put(entry.getKey(), remaining.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP));
            }
        }
        return diff;
    }

    private static Map<UUID, Map<Unit, BigDecimal>> canonicalizeInventory(
        Map<UUID, Map<Unit, BigDecimal>> raw
    ) {
        Map<UUID, Map<Unit, BigDecimal>> result = new HashMap<>(raw.size());
        for (Map.Entry<UUID, Map<Unit, BigDecimal>> ingredientEntry : raw.entrySet()) {
            Map<Unit, BigDecimal> normalized = new HashMap<>();
            for (Map.Entry<Unit, BigDecimal> unitEntry : ingredientEntry.getValue().entrySet()) {
                Unit canonical = UnitConverter.canonical(unitEntry.getKey());
                BigDecimal canonicalAmount = UnitConverter.toCanonical(
                    unitEntry.getValue(), unitEntry.getKey());
                normalized.merge(canonical, canonicalAmount, BigDecimal::add);
            }
            result.put(ingredientEntry.getKey(), normalized);
        }
        return result;
    }

    // --- Mapping --------------------------------------------------------

    private ShoppingListDto toDto(ShoppingList list) {
        Set<UUID> ingredientIds = new HashSet<>();
        for (ShoppingListItem item : list.getItems()) {
            ingredientIds.add(item.getIngredientId());
        }
        Map<UUID, IngredientDto> ingredients = ingredientService.getByIds(ingredientIds);

        List<ShoppingListItemDto> dtos = new ArrayList<>(list.getItems().size());
        for (ShoppingListItem item : list.getItems()) {
            IngredientDto ing = ingredients.get(item.getIngredientId());
            String name = ing != null ? ing.name() : "(unbekannt)";
            IngredientCategory category = ing != null ? ing.category() : IngredientCategory.SONSTIGES;
            dtos.add(ShoppingListItemDto.from(item, name, category));
        }
        // Sortierung im Frontend egal — aber alphabetisch nach Name ist intuitiv.
        dtos.sort((a, b) -> a.ingredientName().compareToIgnoreCase(b.ingredientName()));
        return ShoppingListDto.from(list, dtos);
    }
}
