package de.eateasy.common.units;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class UnitConverterTest {

    @Test
    @DisplayName("TBSP → ML mit Faktor 15")
    void tbspToMl() {
        assertThat(UnitConverter.canonical(Unit.TBSP)).isEqualTo(Unit.ML);
        assertThat(UnitConverter.toCanonical(new BigDecimal("2"), Unit.TBSP))
            .isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("TSP → ML mit Faktor 5")
    void tspToMl() {
        assertThat(UnitConverter.canonical(Unit.TSP)).isEqualTo(Unit.ML);
        assertThat(UnitConverter.toCanonical(new BigDecimal("3"), Unit.TSP))
            .isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("GRAM bleibt GRAM, Faktor 1")
    void gramUnchanged() {
        assertThat(UnitConverter.canonical(Unit.GRAM)).isEqualTo(Unit.GRAM);
        assertThat(UnitConverter.toCanonical(new BigDecimal("100"), Unit.GRAM))
            .isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("ML bleibt ML")
    void mlUnchanged() {
        assertThat(UnitConverter.canonical(Unit.ML)).isEqualTo(Unit.ML);
        assertThat(UnitConverter.toCanonical(new BigDecimal("250"), Unit.ML))
            .isEqualByComparingTo("250");
    }

    @Test
    @DisplayName("PIECE bleibt PIECE (keine Konvertierung sinnvoll)")
    void pieceUnchanged() {
        assertThat(UnitConverter.canonical(Unit.PIECE)).isEqualTo(Unit.PIECE);
        assertThat(UnitConverter.toCanonical(new BigDecimal("4"), Unit.PIECE))
            .isEqualByComparingTo("4");
    }
}
