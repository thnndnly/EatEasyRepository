package de.eateasy.common.units;

import de.eateasy.common.units.UnitParser.UnitParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UnitParserTest {

    @Test
    @DisplayName("null/leeres Token faellt auf defaultUnit mit Multiplier 1.0 zurueck")
    void blankFallsBackToDefault() {
        assertThat(UnitParser.parse(null, Unit.PIECE))
            .isEqualTo(new UnitParseResult(Unit.PIECE, 1.0));
        assertThat(UnitParser.parse("", Unit.GRAM))
            .isEqualTo(new UnitParseResult(Unit.GRAM, 1.0));
        assertThat(UnitParser.parse("   ", Unit.ML))
            .isEqualTo(new UnitParseResult(Unit.ML, 1.0));
    }

    @Test
    @DisplayName("unbekanntes Token faellt auf defaultUnit zurueck")
    void unknownFallsBack() {
        assertThat(UnitParser.parse("xyz", Unit.PIECE))
            .isEqualTo(new UnitParseResult(Unit.PIECE, 1.0));
    }

    @Test
    @DisplayName("kg wird in GRAM mit Multiplier 1000 konvertiert")
    void kilogramConvertsToGramWithMultiplier1000() {
        UnitParseResult result = UnitParser.parse("kg", Unit.PIECE);

        assertThat(result.unit()).isEqualTo(Unit.GRAM);
        assertThat(result.multiplier()).isEqualTo(1000.0);
    }

    @Test
    @DisplayName("l wird in ML mit Multiplier 1000 konvertiert")
    void literConvertsToMlWithMultiplier1000() {
        UnitParseResult result = UnitParser.parse("l", Unit.PIECE);

        assertThat(result.unit()).isEqualTo(Unit.ML);
        assertThat(result.multiplier()).isEqualTo(1000.0);
    }

    @ParameterizedTest
    @CsvSource({
        "g, GRAM, 1.0",
        "gr, GRAM, 1.0",
        "gram, GRAM, 1.0",
        "grams, GRAM, 1.0",
        "gramme, GRAM, 1.0",
        "ml, ML, 1.0",
        "milliliter, ML, 1.0",
        "liter, ML, 1000.0",
        "litre, ML, 1000.0",
        "kilogram, GRAM, 1000.0",
        "kilograms, GRAM, 1000.0",
        "tbsp, TBSP, 1.0",
        "tablespoon, TBSP, 1.0",
        "el, TBSP, 1.0",
        "EL, TBSP, 1.0",
        "tsp, TSP, 1.0",
        "teaspoon, TSP, 1.0",
        "tl, TSP, 1.0",
        "piece, PIECE, 1.0",
        "pcs, PIECE, 1.0",
        "stück, PIECE, 1.0",
        "stueck, PIECE, 1.0"
    })
    @DisplayName("parametrisierte Token-Mappings")
    void knownTokens(String token, Unit expectedUnit, double expectedMultiplier) {
        UnitParseResult result = UnitParser.parse(token, Unit.PIECE);

        assertThat(result.unit()).isEqualTo(expectedUnit);
        assertThat(result.multiplier()).isEqualTo(expectedMultiplier);
    }

    @Test
    @DisplayName("case-insensitive: KG entspricht kg")
    void caseInsensitive() {
        assertThat(UnitParser.parse("KG", Unit.PIECE))
            .isEqualTo(new UnitParseResult(Unit.GRAM, 1000.0));
    }
}
