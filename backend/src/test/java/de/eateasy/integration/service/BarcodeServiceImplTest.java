package de.eateasy.integration.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.repository.UserRepository;
import de.eateasy.auth.service.AuthService;
import de.eateasy.common.exception.NotFoundException;
import de.eateasy.common.units.Unit;
import de.eateasy.household.dto.HouseholdCreateRequest;
import de.eateasy.household.dto.HouseholdDto;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.household.service.HouseholdService;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.integration.client.OpenFoodFactsClient;
import de.eateasy.integration.client.OpenFoodFactsResponse;
import de.eateasy.integration.client.OpenFoodFactsResponse.Product;
import de.eateasy.integration.dto.BarcodePantryRequest;
import de.eateasy.integration.dto.BarcodeProductDto;
import de.eateasy.pantry.dto.PantryItemDto;
import de.eateasy.pantry.repository.PantryItemRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class BarcodeServiceImplTest {

    @Inject
    BarcodeService barcodeService;

    @Inject
    AuthService authService;

    @Inject
    HouseholdService householdService;

    @InjectMock
    @RestClient
    OpenFoodFactsClient openFoodFactsClient;

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
    @DisplayName("lookup liefert deutsche Bezeichnung und abgeleitete Unit")
    void lookupHappyPath() {
        when(openFoodFactsClient.getProduct("123")).thenReturn(
            new OpenFoodFactsResponse("123", 1,
                new Product("Hazelnut Spread", "Nuss-Nougat-Creme", "400 g")));

        BarcodeProductDto dto = barcodeService.lookup("123");

        assertThat(dto.name()).isEqualTo("Nuss-Nougat-Creme");
        assertThat(dto.suggestedUnit()).isEqualTo(Unit.GRAM);
        assertThat(dto.barcode()).isEqualTo("123");
    }

    @Test
    @DisplayName("lookup wirft NotFoundException bei unbekanntem Barcode")
    void lookupNotFound() {
        when(openFoodFactsClient.getProduct(any())).thenReturn(
            new OpenFoodFactsResponse("000", 0, null));

        assertThatThrownBy(() -> barcodeService.lookup("000"))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("addToPantry legt PantryItem an und Zutat wird angelegt")
    void addToPantryHappyPath() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = createHousehold(userId, "Alice WG").id();
        when(openFoodFactsClient.getProduct("555")).thenReturn(
            new OpenFoodFactsResponse("555", 1,
                new Product("Olive Oil", "Olivenoel", "500 ml")));

        PantryItemDto item = barcodeService.addToPantry(userId, householdId,
            new BarcodePantryRequest("555", new BigDecimal("500"), Unit.ML, null));

        assertThat(item.ingredientName()).isEqualTo("Olivenoel");
        assertThat(item.amount()).isEqualByComparingTo("500");
        assertThat(item.unit()).isEqualTo(Unit.ML);
        assertThat(ingredientRepository.findAll().list()).hasSize(1);
        assertThat(pantryRepository.findAll().list()).hasSize(1);
    }

    @Test
    @DisplayName("addToPantry mit unbekanntem Barcode wirft NotFound")
    void addToPantryNotFound() {
        UUID userId = registerUser("alice@example.com");
        UUID householdId = createHousehold(userId, "Alice").id();
        when(openFoodFactsClient.getProduct(any())).thenReturn(
            new OpenFoodFactsResponse("000", 0, null));

        assertThatThrownBy(() -> barcodeService.addToPantry(userId, householdId,
            new BarcodePantryRequest("000", new BigDecimal("1"), Unit.PIECE, null)))
            .isInstanceOf(NotFoundException.class);

        assertThat(pantryRepository.findAll().list()).isEmpty();
    }

    // --- Helpers ---------------------------------------------------------

    private UUID registerUser(String email) {
        AuthResponse response = authService.register(
            new RegisterRequest(email, "secret12", email));
        return response.user().id();
    }

    private HouseholdDto createHousehold(UUID userId, String name) {
        return householdService.create(userId, new HouseholdCreateRequest(name, null));
    }
}
