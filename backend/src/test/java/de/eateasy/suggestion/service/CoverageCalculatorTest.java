package de.eateasy.suggestion.service;

import de.eateasy.common.units.Unit;
import de.eateasy.recipe.dto.RecipeIngredientView;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageCalculatorTest {

    @Test
    @DisplayName("Alle Zutaten vorhanden → coverage = 1.0")
    void fullCoverage() {
        UUID salt = UUID.randomUUID();
        UUID pepper = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        Map<UUID, Double> coverage = CoverageCalculator.compute(
            Map.of(recipeId, List.of(view(salt), view(pepper))),
            Set.of(salt, pepper));

        assertThat(coverage.get(recipeId)).isEqualTo(1.0, Offset.offset(0.001));
    }

    @Test
    @DisplayName("Haelfte der Zutaten vorhanden → coverage = 0.5")
    void halfCoverage() {
        UUID salt = UUID.randomUUID();
        UUID pepper = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        Map<UUID, Double> coverage = CoverageCalculator.compute(
            Map.of(recipeId, List.of(view(salt), view(pepper))),
            Set.of(salt));

        assertThat(coverage.get(recipeId)).isEqualTo(0.5, Offset.offset(0.001));
    }

    @Test
    @DisplayName("Keine Zutat vorhanden → coverage = 0")
    void zeroCoverage() {
        UUID salt = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        Map<UUID, Double> coverage = CoverageCalculator.compute(
            Map.of(recipeId, List.of(view(salt))),
            Set.of(UUID.randomUUID()));

        assertThat(coverage.get(recipeId)).isEqualTo(0.0, Offset.offset(0.001));
    }

    @Test
    @DisplayName("Leere Zutatenliste → coverage = 0 (kein Match moeglich)")
    void emptyIngredientsZero() {
        UUID recipeId = UUID.randomUUID();

        Map<UUID, Double> coverage = CoverageCalculator.compute(
            Map.of(recipeId, List.of()),
            Set.of(UUID.randomUUID()));

        assertThat(coverage.get(recipeId)).isEqualTo(0.0, Offset.offset(0.001));
    }

    @Test
    @DisplayName("Mehrere Rezepte → unabhaengige Coverage-Werte")
    void multipleRecipes() {
        UUID r1 = UUID.randomUUID();
        UUID r2 = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        Map<UUID, Double> coverage = CoverageCalculator.compute(
            Map.of(
                r1, List.of(view(a), view(b)),
                r2, List.of(view(a), view(b), view(c))),
            Set.of(a, b));

        assertThat(coverage.get(r1)).isEqualTo(1.0, Offset.offset(0.001));
        assertThat(coverage.get(r2)).isEqualTo(2.0 / 3, Offset.offset(0.001));
    }

    private static RecipeIngredientView view(UUID ingredientId) {
        return new RecipeIngredientView(ingredientId, BigDecimal.ONE, Unit.PIECE);
    }
}
