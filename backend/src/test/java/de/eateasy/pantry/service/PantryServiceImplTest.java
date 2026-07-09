package de.eateasy.pantry.service;

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
import de.eateasy.pantry.dto.AddPantryItemRequest;
import de.eateasy.pantry.dto.PantryItemDto;
import de.eateasy.pantry.dto.UpdatePantryItemRequest;
import de.eateasy.pantry.repository.PantryItemRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class PantryServiceImplTest {

    @Inject
    PantryService pantryService;

    @Inject
    HouseholdService householdService;

    @Inject
    AuthService authService;

    @Inject
    PantryItemRepository pantryRepository;

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
        pantryRepository.deleteAll();
        ingredientRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @TestTransaction
    @DisplayName("add legt einen neuen Eintrag mit Auto-Anlage der Zutat an")
    void addCreatesNewItem() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = householdService.create(userId,
            new HouseholdCreateRequest("Test", null)).id();

        PantryItemDto item = pantryService.add(userId, householdId, new AddPantryItemRequest(
            null, "Tomate", new BigDecimal("500"), Unit.GRAM, LocalDate.of(2026, 6, 1)));

        assertThat(item.id()).isNotNull();
        assertThat(item.ingredientName()).isEqualTo("Tomate");
        assertThat(item.amount()).isEqualByComparingTo("500");
        assertThat(item.unit()).isEqualTo(Unit.GRAM);
        assertThat(item.bestBefore()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    @TestTransaction
    @DisplayName("add aggregiert Mengen bei gleicher Zutat + Unit")
    void addAggregatesSameUnit() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = householdService.create(userId,
            new HouseholdCreateRequest("Test", null)).id();

        PantryItemDto first = pantryService.add(userId, householdId, new AddPantryItemRequest(
            null, "Tomate", new BigDecimal("500"), Unit.GRAM, null));
        PantryItemDto second = pantryService.add(userId, householdId, new AddPantryItemRequest(
            null, "Tomate", new BigDecimal("250"), Unit.GRAM, null));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.amount()).isEqualByComparingTo("750");

        List<PantryItemDto> all = pantryService.list(userId, householdId);
        assertThat(all).hasSize(1);
    }

    @Test
    @TestTransaction
    @DisplayName("add legt neuen Eintrag bei gleicher Zutat aber anderer Unit an")
    void addNewItemForDifferentUnit() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = householdService.create(userId,
            new HouseholdCreateRequest("Test", null)).id();

        pantryService.add(userId, householdId, new AddPantryItemRequest(
            null, "Tomate", new BigDecimal("500"), Unit.GRAM, null));
        pantryService.add(userId, householdId, new AddPantryItemRequest(
            null, "Tomate", new BigDecimal("3"), Unit.PIECE, null));

        List<PantryItemDto> all = pantryService.list(userId, householdId);
        assertThat(all).hasSize(2);
        assertThat(all).extracting(PantryItemDto::unit)
            .containsExactlyInAnyOrder(Unit.GRAM, Unit.PIECE);
    }

    @Test
    @TestTransaction
    @DisplayName("add für fremden Haushalt wirft Forbidden")
    void addForbiddenForOutsider() {
        UUID alice = registerUser("alice@example.com");
        UUID bob = registerUser("bob@example.com");
        UUID aliceHouse = householdService.create(alice,
            new HouseholdCreateRequest("Alice", null)).id();

        assertThatThrownBy(() -> pantryService.add(bob, aliceHouse, new AddPantryItemRequest(
            null, "Salz", BigDecimal.ONE, Unit.GRAM, null)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("list ist sortiert nach MHD ascending, NULL ans Ende")
    void listSortedByBestBefore() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = householdService.create(userId,
            new HouseholdCreateRequest("Test", null)).id();

        pantryService.add(userId, householdId, new AddPantryItemRequest(
            null, "OhneMHD", BigDecimal.ONE, Unit.PIECE, null));
        pantryService.add(userId, householdId, new AddPantryItemRequest(
            null, "Später", BigDecimal.ONE, Unit.PIECE, LocalDate.of(2026, 12, 31)));
        pantryService.add(userId, householdId, new AddPantryItemRequest(
            null, "Früher", BigDecimal.ONE, Unit.PIECE, LocalDate.of(2026, 6, 1)));

        List<PantryItemDto> list = pantryService.list(userId, householdId);

        assertThat(list).extracting(PantryItemDto::ingredientName)
            .containsExactly("Früher", "Später", "OhneMHD");
    }

    @Test
    @TestTransaction
    @DisplayName("update ändert Menge und MHD")
    void updateChangesFields() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = householdService.create(userId,
            new HouseholdCreateRequest("Test", null)).id();
        PantryItemDto item = pantryService.add(userId, householdId, new AddPantryItemRequest(
            null, "Tomate", new BigDecimal("500"), Unit.GRAM, null));

        PantryItemDto updated = pantryService.update(userId, item.id(), new UpdatePantryItemRequest(
            new BigDecimal("250"), null, LocalDate.of(2026, 7, 1)));

        assertThat(updated.amount()).isEqualByComparingTo("250");
        assertThat(updated.bestBefore()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(updated.unit()).isEqualTo(Unit.GRAM);
    }

    @Test
    @TestTransaction
    @DisplayName("update für fremden Eintrag wirft Forbidden")
    void updateForbiddenForOutsider() {
        UUID alice = registerUser("alice@example.com");
        UUID bob = registerUser("bob@example.com");
        UUID aliceHouse = householdService.create(alice,
            new HouseholdCreateRequest("Alice", null)).id();
        PantryItemDto aliceItem = pantryService.add(alice, aliceHouse, new AddPantryItemRequest(
            null, "Salz", BigDecimal.ONE, Unit.GRAM, null));

        assertThatThrownBy(() -> pantryService.update(bob, aliceItem.id(),
            new UpdatePantryItemRequest(BigDecimal.TEN, null, null)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("delete entfernt den Eintrag")
    void deleteRemoves() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = householdService.create(userId,
            new HouseholdCreateRequest("Test", null)).id();
        PantryItemDto item = pantryService.add(userId, householdId, new AddPantryItemRequest(
            null, "Salz", BigDecimal.ONE, Unit.GRAM, null));

        pantryService.delete(userId, item.id());

        assertThat(pantryService.list(userId, householdId)).isEmpty();
    }

    private UUID registerUser(String email) {
        AuthResponse response = authService.register(
            new RegisterRequest(email, "secret12", email));
        return response.user().id();
    }
}
