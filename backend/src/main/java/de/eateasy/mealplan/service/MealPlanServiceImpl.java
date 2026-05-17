package de.eateasy.mealplan.service;

import de.eateasy.common.exception.NotFoundException;
import de.eateasy.household.service.HouseholdService;
import de.eateasy.mealplan.dto.MealPlanDto;
import de.eateasy.mealplan.dto.MealPlanEntryDto;
import de.eateasy.mealplan.dto.SetEntryRequest;
import de.eateasy.mealplan.entity.MealPlan;
import de.eateasy.mealplan.entity.MealPlanEntry;
import de.eateasy.mealplan.entity.MealType;
import de.eateasy.mealplan.repository.MealPlanEntryRepository;
import de.eateasy.mealplan.repository.MealPlanRepository;
import de.eateasy.recipe.dto.RecipeMiniDto;
import de.eateasy.recipe.service.RecipeService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class MealPlanServiceImpl implements MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanEntryRepository entryRepository;
    private final HouseholdService householdService;
    private final RecipeService recipeService;

    public MealPlanServiceImpl(MealPlanRepository mealPlanRepository,
                               MealPlanEntryRepository entryRepository,
                               HouseholdService householdService,
                               RecipeService recipeService) {
        this.mealPlanRepository = mealPlanRepository;
        this.entryRepository = entryRepository;
        this.householdService = householdService;
        this.recipeService = recipeService;
    }

    @Override
    @Transactional
    public MealPlanDto getOrCreate(UUID userId, UUID householdId, LocalDate anyDateInWeek) {
        householdService.assertMember(userId, householdId);

        LocalDate monday = normalizeToMonday(anyDateInWeek == null ? LocalDate.now() : anyDateInWeek);
        MealPlan plan = mealPlanRepository.findByHouseholdAndWeek(householdId, monday)
            .orElseGet(() -> {
                MealPlan created = new MealPlan(householdId, monday);
                mealPlanRepository.persist(created);
                return created;
            });

        return toDto(plan);
    }

    @Override
    @Transactional
    public MealPlanDto getCurrent(UUID userId, UUID householdId) {
        return getOrCreate(userId, householdId, LocalDate.now());
    }

    @Override
    @Transactional
    public MealPlanDto getById(UUID userId, UUID mealPlanId) {
        MealPlan plan = loadPlan(mealPlanId);
        householdService.assertMember(userId, plan.getHouseholdId());
        return toDto(plan);
    }

    @Override
    @Transactional
    public MealPlanEntryDto setEntry(UUID userId, UUID mealPlanId, SetEntryRequest request) {
        MealPlan plan = loadPlan(mealPlanId);
        householdService.assertMember(userId, plan.getHouseholdId());
        // RecipeService.get prueft Sichtbarkeit/Auth fuer das Rezept aus User-Sicht.
        recipeService.get(userId, request.recipeId());

        Optional<MealPlanEntry> existing = plan.getEntries().stream()
            .filter(e -> e.getDayOfWeek() == request.dayOfWeek()
                && e.getMealType() == request.mealType())
            .findFirst();

        MealPlanEntry entry;
        if (existing.isPresent()) {
            entry = existing.get();
            entry.setRecipeId(request.recipeId());
            entry.setServings(request.servings());
        } else {
            entry = new MealPlanEntry(
                request.dayOfWeek(),
                request.mealType(),
                request.recipeId(),
                request.servings());
            plan.addEntry(entry);
        }

        // Damit der Eintrag eine ID hat, falls er neu war.
        entryRepository.flush();

        Map<UUID, RecipeMiniDto> minis = recipeService.getMinis(List.of(request.recipeId()));
        return MealPlanEntryDto.from(entry, minis.get(request.recipeId()));
    }

    @Override
    @Transactional
    public void removeEntry(UUID userId, UUID mealPlanId, DayOfWeek day, MealType mealType) {
        MealPlan plan = loadPlan(mealPlanId);
        householdService.assertMember(userId, plan.getHouseholdId());

        plan.getEntries().stream()
            .filter(e -> e.getDayOfWeek() == day && e.getMealType() == mealType)
            .findFirst()
            .ifPresent(plan::removeEntry);
    }

    // --- Helpers ---------------------------------------------------------

    private MealPlan loadPlan(UUID mealPlanId) {
        return mealPlanRepository.findByIdOptional(mealPlanId)
            .orElseThrow(() -> new NotFoundException("Wochenplan nicht gefunden: " + mealPlanId));
    }

    private static LocalDate normalizeToMonday(LocalDate date) {
        int daysFromMonday = date.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        return date.minusDays(daysFromMonday);
    }

    private MealPlanDto toDto(MealPlan plan) {
        Set<UUID> recipeIds = new HashSet<>();
        for (MealPlanEntry e : plan.getEntries()) {
            recipeIds.add(e.getRecipeId());
        }
        Map<UUID, RecipeMiniDto> minis = recipeService.getMinis(recipeIds);

        List<MealPlanEntryDto> entryDtos = new ArrayList<>(plan.getEntries().size());
        for (MealPlanEntry e : plan.getEntries()) {
            entryDtos.add(MealPlanEntryDto.from(e, minis.get(e.getRecipeId())));
        }
        return MealPlanDto.from(plan, entryDtos);
    }
}
