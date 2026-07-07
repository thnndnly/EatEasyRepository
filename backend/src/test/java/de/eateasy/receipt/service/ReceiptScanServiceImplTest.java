package de.eateasy.receipt.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.repository.UserRepository;
import de.eateasy.auth.service.AuthService;
import de.eateasy.common.exception.BadRequestException;
import de.eateasy.common.exception.ForbiddenException;
import de.eateasy.common.exception.ServiceUnavailableException;
import de.eateasy.common.units.Unit;
import de.eateasy.household.dto.HouseholdCreateRequest;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.household.service.HouseholdService;
import de.eateasy.ingredient.dto.IngredientDto;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.ingredient.service.IngredientService;
import de.eateasy.receipt.client.OcrClient;
import de.eateasy.receipt.dto.ReceiptScanResponse;
import de.eateasy.suggestion.client.OllamaClient;
import de.eateasy.suggestion.client.OllamaGenerateResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class ReceiptScanServiceImplTest {

    private static final byte[] IMAGE = new byte[] {1, 2, 3};

    @Inject
    ReceiptScanService receiptScanService;

    @Inject
    HouseholdService householdService;

    @Inject
    IngredientService ingredientService;

    @Inject
    AuthService authService;

    @InjectMock
    OcrClient ocrClient;

    @InjectMock
    OllamaClient ollamaClient;

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
        ingredientRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @TestTransaction
    @DisplayName("scan: OCR + Ollama-JSON wird zu Items mit Ingredient-Match")
    void scanHappyPath() {
        UUID user = registerUser("alice@example.com");
        UUID household = createHousehold(user);
        IngredientDto milch = ingredientService.findOrCreate("Milch", Unit.ML);

        when(ocrClient.extractText(any(), anyString())).thenReturn("REWE\nVollmilch 1L 1,19\nBanane 0,89");
        when(ollamaClient.generate(any())).thenReturn(new OllamaGenerateResponse(
            "llama3",
            "[{\"name\":\"Milch\",\"amount\":1000,\"unit\":\"ML\"},"
                + "{\"name\":\"Banane\",\"amount\":1,\"unit\":\"PIECE\"}]",
            true));

        ReceiptScanResponse result = receiptScanService.scan(user, household, IMAGE, "bon.jpg");

        assertThat(result.rawText()).contains("Vollmilch");
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).name()).isEqualTo("Milch");
        assertThat(result.items().get(0).amount()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(result.items().get(0).unit()).isEqualTo(Unit.ML);
        assertThat(result.items().get(0).ingredientId()).isEqualTo(milch.id());
        // "Banane" existiert noch nicht im Pool -> kein Match
        assertThat(result.items().get(1).ingredientId()).isNull();
    }

    @Test
    @TestTransaction
    @DisplayName("scan: Prosa um das JSON herum wird toleriert")
    void scanTolleratesProseAroundJson() {
        UUID user = registerUser("alice@example.com");
        UUID household = createHousehold(user);

        when(ocrClient.extractText(any(), anyString())).thenReturn("Bon-Text");
        when(ollamaClient.generate(any())).thenReturn(new OllamaGenerateResponse(
            "llama3",
            "Hier ist das Ergebnis:\n[{\"name\":\"Reis\",\"amount\":500,\"unit\":\"GRAM\"}]\nViel Spass!",
            true));

        ReceiptScanResponse result = receiptScanService.scan(user, household, IMAGE, "bon.jpg");

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Reis");
        assertThat(result.items().get(0).unit()).isEqualTo(Unit.GRAM);
    }

    @Test
    @TestTransaction
    @DisplayName("scan: kg/l-Einheiten werden inkl. Multiplikator auf GRAM/ML normalisiert")
    void scanAppliesUnitMultiplier() {
        UUID user = registerUser("alice@example.com");
        UUID household = createHousehold(user);

        when(ocrClient.extractText(any(), anyString())).thenReturn("Bon-Text");
        when(ollamaClient.generate(any())).thenReturn(new OllamaGenerateResponse(
            "llama3",
            "[{\"name\":\"Kartoffeln\",\"amount\":2,\"unit\":\"kg\"},"
                + "{\"name\":\"Milch\",\"amount\":1.5,\"unit\":\"l\"}]",
            true));

        ReceiptScanResponse result = receiptScanService.scan(user, household, IMAGE, "bon.jpg");

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).amount()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(result.items().get(0).unit()).isEqualTo(Unit.GRAM);
        assertThat(result.items().get(1).amount()).isEqualByComparingTo(new BigDecimal("1500"));
        assertThat(result.items().get(1).unit()).isEqualTo(Unit.ML);
    }

    @Test
    @TestTransaction
    @DisplayName("scan: Prosa mit eigenen eckigen Klammern vor dem JSON wird toleriert")
    void scanTolleratesBracketsInProse() {
        UUID user = registerUser("alice@example.com");
        UUID household = createHousehold(user);

        when(ocrClient.extractText(any(), anyString())).thenReturn("Bon-Text");
        when(ollamaClient.generate(any())).thenReturn(new OllamaGenerateResponse(
            "llama3",
            "Hinweis [OCR-Fehler moeglich]: "
                + "[{\"name\":\"Milch\",\"amount\":1000,\"unit\":\"ML\"}] Viel Spass!",
            true));

        ReceiptScanResponse result = receiptScanService.scan(user, household, IMAGE, "bon.jpg");

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Milch");
    }

    @Test
    @TestTransaction
    @DisplayName("scan: Objekt-Wrapper um das Array ({\"items\":[...]}) wird toleriert")
    void scanTolleratesObjectWrapper() {
        UUID user = registerUser("alice@example.com");
        UUID household = createHousehold(user);

        when(ocrClient.extractText(any(), anyString())).thenReturn("Bon-Text");
        when(ollamaClient.generate(any())).thenReturn(new OllamaGenerateResponse(
            "llama3",
            "{\"items\":[{\"name\":\"Reis\",\"amount\":500,\"unit\":\"GRAM\"}]}",
            true));

        ReceiptScanResponse result = receiptScanService.scan(user, household, IMAGE, "bon.jpg");

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Reis");
    }

    @Test
    @TestTransaction
    @DisplayName("scan: Ollama-Fehler -> Rohtext mit leerer Item-Liste statt Exception")
    void scanFallsBackOnOllamaFailure() {
        UUID user = registerUser("alice@example.com");
        UUID household = createHousehold(user);

        when(ocrClient.extractText(any(), anyString())).thenReturn("Bon-Text");
        when(ollamaClient.generate(any())).thenThrow(new RuntimeException("Ollama down"));

        ReceiptScanResponse result = receiptScanService.scan(user, household, IMAGE, "bon.jpg");

        assertThat(result.rawText()).isEqualTo("Bon-Text");
        assertThat(result.items()).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("scan: leerer OCR-Text wirft BadRequest")
    void scanRejectsEmptyOcr() {
        UUID user = registerUser("alice@example.com");
        UUID household = createHousehold(user);

        when(ocrClient.extractText(any(), anyString())).thenReturn("   ");

        assertThatThrownBy(() -> receiptScanService.scan(user, household, IMAGE, "bon.jpg"))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("scan: OCR-Ausfall -> ServiceUnavailable (503) statt unbehandelter 500")
    void scanTranslatesOcrFailureToServiceUnavailable() {
        UUID user = registerUser("alice@example.com");
        UUID household = createHousehold(user);

        when(ocrClient.extractText(any(), anyString()))
            .thenThrow(new RuntimeException("Tesseract-Call fehlgeschlagen: Connection refused"));

        assertThatThrownBy(() -> receiptScanService.scan(user, household, IMAGE, "bon.jpg"))
            .isInstanceOf(ServiceUnavailableException.class)
            // generische Nachricht, keine internen Details wie "Connection refused"
            .hasMessageNotContaining("Connection refused")
            .hasMessageNotContaining("Tesseract");
    }

    @Test
    @TestTransaction
    @DisplayName("scan: Nicht-Mitglied bekommt Forbidden, OCR wird gar nicht erst aufgerufen")
    void scanForbiddenForOutsider() {
        UUID alice = registerUser("alice@example.com");
        UUID bob = registerUser("bob@example.com");
        UUID aliceHousehold = createHousehold(alice);

        assertThatThrownBy(() -> receiptScanService.scan(bob, aliceHousehold, IMAGE, "bon.jpg"))
            .isInstanceOf(ForbiddenException.class);
    }

    // --- Helpers ---------------------------------------------------------

    private UUID registerUser(String email) {
        AuthResponse response = authService.register(
            new RegisterRequest(email, "secret12", email));
        return response.user().id();
    }

    private UUID createHousehold(UUID ownerId) {
        return householdService.create(ownerId, new HouseholdCreateRequest("Test", null)).id();
    }
}
