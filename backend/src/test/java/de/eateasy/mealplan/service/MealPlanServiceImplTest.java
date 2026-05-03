package de.eateasy.mealplan.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.repository.UserRepository;
import de.eateasy.auth.service.AuthService;
import de.eateasy.common.exception.ForbiddenException;
import de.eateasy.common.units.Unit;
import de.eateasy.household.dto.HouseholdCreateRequest;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.household.service.HouseholdService;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.mealplan.dto.MealPlanDto;
import de.eateasy.mealplan.dto.MealPlanEntryDto;
import de.eateasy.mealplan.dto.SetEntryRequest;
import de.eateasy.mealplan.entity.MealType;
import de.eateasy.mealplan.repository.MealPlanEntryRepository;
import de.eateasy.mealplan.repository.MealPlanRepository;
import de.eateasy.recipe.dto.RecipeCreateRequest;
import de.eateasy.recipe.dto.RecipeDto;
import de.eateasy.recipe.dto.RecipeIngredientRequest;
import de.eateasy.recipe.repository.RecipeRepository;
import de.eateasy.recipe.service.RecipeService;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class MealPlanServiceImplTest {

    @Inject
    MealPlanService mealPlanService;

    @Inject
    HouseholdService householdService;

    @Inject
    RecipeService recipeService;

    @Inject
    AuthService authService;

    @Inject
    MealPlanEntryRepository entryRepository;

    @Inject
    MealPlanRepository mealPlanRepository;

    @Inject
    RecipeRepository recipeRepository;

    @Inject
    IngredientRepository ingredientRepository;

    @Inject
    HouseholdInvitationRepository invitationRepository;

    @Inject
    HouseholdMembershipRepository membershipRepository;

    @Inject
    HouseholdRepository householdRepository;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    @Transactional
    void cleanUp() {
        entryRepository.deleteAll();
        mealPlanRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @TestTransaction
    @DisplayName("getOrCreate normalisiert auf Montag und legt Plan lazy an")
    void getOrCreateNormalizesAndCreates() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = householdService.create(userId,
            new HouseholdCreateRequest("Test", null)).id();

        // Mittwoch der KW 18/2026 → Montag = 2026-04-27.
        LocalDate wednesday = LocalDate.of(2026, 4, 29);
        MealPlanDto plan = mealPlanService.getOrCreate(userId, householdId, wednesday);

        assertThat(plan.weekStart()).isEqualTo(LocalDate.of(2026, 4, 27));
        assertThat(plan.entries()).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("getOrCreate ist idempotent fuer dieselbe Woche")
    void getOrCreateIdempotent() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = householdService.create(userId,
            new HouseholdCreateRequest("Test", null)).id();

        MealPlanDto first = mealPlanService.getOrCreate(userId, householdId,
            LocalDate.of(2026, 4, 27));
        MealPlanDto second = mealPlanService.getOrCreate(userId, householdId,
            LocalDate.of(2026, 4, 30));

        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    @TestTransaction
    @DisplayName("getOrCreate fuer fremden Haushalt wirft Forbidden")
    void getOrCreateForbiddenForOutsider() {
        UUID alice = registerUser("alice@example.com");
        UUID bob = registerUser("bob@example.com");
        UUID householdId = householdService.create(alice,
            new HouseholdCreateRequest("Alice Haus", null)).id();

        assertThatThrownBy(() -> mealPlanService.getOrCreate(bob, householdId,
            LocalDate.of(2026, 4, 27)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("setEntry legt Slot an und Recipe-Mini-DTO ist eingebettet")
    void setEntryCreates() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = householdService.create(userId,
            new HouseholdCreateRequest("Test", null)).id();
        RecipeDto recipe = createRecipe(userId, householdId, "Tomatensuppe");
        MealPlanDto plan = mealPlanService.getOrCreate(userId, householdId,
            LocalDate.of(2026, 4, 27));

        MealPlanEntryDto entry = mealPlanService.setEntry(userId, plan.id(),
            new SetEntryRequest(DayOfWeek.MONDAY, MealType.LUNCH, recipe.id(), 4));

        assertThat(entry.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(entry.mealType()).isEqualTo(MealType.LUNCH);
        assertThat(entry.servings()).isEqualTo(4);
        assertThat(entry.recipe()).isNotNull();
        assertThat(entry.recipe().title()).isEqualTo("Tomatensuppe");
    }

    @Test
    @TestTransaction
    @DisplayName("setEntry ueberschreibt vorhandenen Slot (Unique-Constraint)")
    void setEntryOverwrites() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = householdService.create(userId,
            new HouseholdCreateRequest("Test", null)).id();
        RecipeDto first = createRecipe(userId, householdId, "Erstes");
        RecipeDto second = createRecipe(userId, householdId, "Zweites");
        MealPlanDto plan = mealPlanService.getOrCreate(userId, householdId,
            LocalDate.of(2026, 4, 27));

        mealPlanService.setEntry(userId, plan.id(),
            new SetEntryRequest(DayOfWeek.MONDAY, MealType.LUNCH, first.id(), 2));
        MealPlanEntryDto updated = mealPlanService.setEntry(userId, plan.id(),
            new SetEntryRequest(DayOfWeek.MONDAY, MealType.LUNCH, second.id(), 6));

        assertThat(updated.recipe().title()).isEqualTo("Zweites");
        assertThat(updated.servings()).isEqualTo(6);

        MealPlanDto reloaded = mealPlanService.getOrCreate(userId, householdId,
            LocalDate.of(2026, 4, 27));
        assertThat(reloaded.entries()).hasSize(1);
    }

    @Test
    @TestTransaction
    @DisplayName("setEntry fuer fremden Haushalt wirft Forbidden")
    void setEntryForbiddenForOutsider() {
        UUID alice = registerUser("alice@example.com");
        UUID bob = registerUser("bob@example.com");
        UUID householdId = householdService.create(alice,
            new HouseholdCreateRequest("Alice Haus", null)).id();
        RecipeDto recipe = createRecipe(alice, householdId, "Suppe");
        MealPlanDto plan = mealPlanService.getOrCreate(alice, householdId,
            LocalDate.of(2026, 4, 27));

        assertThatThrownBy(() -> mealPlanService.setEntry(bob, plan.id(),
            new SetEntryRequest(DayOfWeek.MONDAY, MealType.LUNCH, recipe.id(), 2)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("setEntry mit fremdem Rezept wirft Forbidden ueber RecipeService.get")
    void setEntryForbiddenForeignRecipe() {
        UUID alice = registerUser("alice@example.com");
        UUID bob = registerUser("bob@example.com");
        UUID aliceHouse = householdService.create(alice,
            new HouseholdCreateRequest("Alice Haus", null)).id();
        UUID bobHouse = householdService.create(bob,
            new HouseholdCreateRequest("Bob Haus", null)).id();
        RecipeDto bobRecipe = createRecipe(bob, bobHouse, "Bob Suppe");
        MealPlanDto plan = mealPlanService.getOrCreate(alice, aliceHouse,
            LocalDate.of(2026, 4, 27));

        assertThatThrownBy(() -> mealPlanService.setEntry(alice, plan.id(),
            new SetEntryRequest(DayOfWeek.MONDAY, MealType.LUNCH, bobRecipe.id(), 2)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("removeEntry loescht den Slot")
    void removeEntry() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = householdService.create(userId,
            new HouseholdCreateRequest("Test", null)).id();
        RecipeDto recipe = createRecipe(userId, householdId, "Suppe");
        MealPlanDto plan = mealPlanService.getOrCreate(userId, householdId,
            LocalDate.of(2026, 4, 27));
        mealPlanService.setEntry(userId, plan.id(),
            new SetEntryRequest(DayOfWeek.TUESDAY, MealType.DINNER, recipe.id(), 2));

        mealPlanService.removeEntry(userId, plan.id(), DayOfWeek.TUESDAY, MealType.DINNER);

        MealPlanDto reloaded = mealPlanService.getOrCreate(userId, householdId,
            LocalDate.of(2026, 4, 27));
        assertThat(reloaded.entries()).isEmpty();
    }

    // --- Helpers ---------------------------------------------------------

    private UUID registerUser(String email) {
        AuthResponse response = authService.register(
            new RegisterRequest(email, "secret12", email));
        return response.user().id();
    }

    private RecipeDto createRecipe(UUID userId, UUID householdId, String title) {
        return recipeService.create(userId, new RecipeCreateRequest(
            title, null, "Steps", 2, null, null, householdId,
            List.of(new RecipeIngredientRequest(null, "Salz", new BigDecimal("5"), Unit.GRAM, null))));
    }
}
