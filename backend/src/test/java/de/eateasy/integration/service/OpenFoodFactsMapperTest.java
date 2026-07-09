package de.eateasy.integration.service;

import de.eateasy.common.units.Unit;
import de.eateasy.integration.client.OpenFoodFactsResponse;
import de.eateasy.integration.client.OpenFoodFactsResponse.Product;
import de.eateasy.integration.dto.BarcodeProductDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenFoodFactsMapperTest {

    @Test
    @DisplayName("Deutscher Name bevorzugt")
    void prefersGermanName() {
        OpenFoodFactsResponse r = response(1, new Product("Nutella", "Nuss-Nougat-Creme", "400 g"));

        BarcodeProductDto dto = OpenFoodFactsMapper.toDto("123", r);

        assertThat(dto).isNotNull();
        assertThat(dto.name()).isEqualTo("Nuss-Nougat-Creme");
        assertThat(dto.suggestedUnit()).isEqualTo(Unit.GRAM);
        assertThat(dto.barcode()).isEqualTo("123");
    }

    @Test
    @DisplayName("Fällt auf product_name zurück, wenn product_name_de fehlt")
    void fallsBackToDefaultName() {
        OpenFoodFactsResponse r = response(1, new Product("Olive Oil", null, "500 ml"));

        BarcodeProductDto dto = OpenFoodFactsMapper.toDto("99", r);

        assertThat(dto.name()).isEqualTo("Olive Oil");
        assertThat(dto.suggestedUnit()).isEqualTo(Unit.ML);
    }

    @Test
    @DisplayName("Status 0 → null (nicht gefunden)")
    void status0ReturnsNull() {
        OpenFoodFactsResponse r = response(0, null);
        assertThat(OpenFoodFactsMapper.toDto("000", r)).isNull();
    }

    @Test
    @DisplayName("Null-Antwort → null")
    void nullResponseReturnsNull() {
        assertThat(OpenFoodFactsMapper.toDto("000", null)).isNull();
    }

    @Test
    @DisplayName("Fehlende Namen → 'Produkt <barcode>'")
    void fallbackNameWhenAllMissing() {
        OpenFoodFactsResponse r = response(1, new Product(null, null, null));

        BarcodeProductDto dto = OpenFoodFactsMapper.toDto("777", r);

        assertThat(dto.name()).isEqualTo("Produkt 777");
        assertThat(dto.suggestedUnit()).isEqualTo(Unit.PIECE);
    }

    @Nested
    class UnitDerivation {

        @Test
        @DisplayName("'400 g' → GRAM")
        void grams() {
            assertThat(OpenFoodFactsMapper.deriveUnit("400 g")).isEqualTo(Unit.GRAM);
        }

        @Test
        @DisplayName("'1.5 kg' → GRAM")
        void kg() {
            assertThat(OpenFoodFactsMapper.deriveUnit("1.5 kg")).isEqualTo(Unit.GRAM);
        }

        @Test
        @DisplayName("'500 ml' → ML")
        void milliliters() {
            assertThat(OpenFoodFactsMapper.deriveUnit("500 ml")).isEqualTo(Unit.ML);
        }

        @Test
        @DisplayName("'1 l' → ML")
        void liters() {
            assertThat(OpenFoodFactsMapper.deriveUnit("1 l")).isEqualTo(Unit.ML);
        }

        @Test
        @DisplayName("Leerer/unbekannter Quantity-String → PIECE")
        void unknownUnit() {
            assertThat(OpenFoodFactsMapper.deriveUnit(null)).isEqualTo(Unit.PIECE);
            assertThat(OpenFoodFactsMapper.deriveUnit("")).isEqualTo(Unit.PIECE);
            assertThat(OpenFoodFactsMapper.deriveUnit("2 Stück")).isEqualTo(Unit.PIECE);
        }
    }

    private static OpenFoodFactsResponse response(int status, Product product) {
        return new OpenFoodFactsResponse("test-code", status, product);
    }
}
