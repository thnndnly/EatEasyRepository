package de.eateasy.integration.service;

import de.eateasy.common.diet.DietTag;
import de.eateasy.common.units.Unit;
import de.eateasy.integration.client.TheMealDbResponse.TheMealDbMeal;
import de.eateasy.recipe.dto.RecipeCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reiner Unit-Test ohne Quarkus-Boot — der Mapper ist eine pure Funktion.
 */
class TheMealDbMapperTest {

    @Test
    @DisplayName("toCreateRequest mappt Titel/Anleitung und parst Mengen mit Suffixen")
    void mapsBasicFields() {
        TheMealDbMeal meal = meal(
            "52772", "Teriyaki Chicken Casserole", "Chicken", "Japanese",
            "Step 1. Step 2.",
            "soy sauce", "1/2 cup",
            "rice", "200g",
            "salt", "1 tsp"
        );

        RecipeCreateRequest req = TheMealDbMapper.toCreateRequest(meal, null);

        assertThat(req.title()).isEqualTo("Teriyaki Chicken Casserole");
        assertThat(req.description()).isEqualTo("Chicken · Japanese");
        assertThat(req.instructions()).isEqualTo("Step 1. Step 2.");
        assertThat(req.servings()).isEqualTo(4);
        assertThat(req.ingredients()).hasSize(3);

        assertThat(req.ingredients().get(1).ingredientName()).isEqualTo("rice");
        assertThat(req.ingredients().get(1).amount()).isEqualByComparingTo("200");
        assertThat(req.ingredients().get(1).unit()).isEqualTo(Unit.GRAM);

        assertThat(req.ingredients().get(2).ingredientName()).isEqualTo("salt");
        assertThat(req.ingredients().get(2).unit()).isEqualTo(Unit.TSP);
    }

    @Test
    @DisplayName("Vegan-Kategorie liefert vegan + vegetarian + dairy_free Tags")
    void veganCategoryMapsToTags() {
        TheMealDbMeal meal = meal(
            "1", "Vegan Salad", "Vegan", "International",
            "Mix.",
            "lettuce", "1");

        RecipeCreateRequest req = TheMealDbMapper.toCreateRequest(meal, null);

        assertThat(req.dietTags())
            .containsExactlyInAnyOrder(DietTag.VEGAN, DietTag.VEGETARIAN, DietTag.DAIRY_FREE);
    }

    @Test
    @DisplayName("Nicht parsbare Measure landet als 1 PIECE mit Original als note")
    void unparseableMeasureBecomesPieceWithNote() {
        TheMealDbMeal meal = meal(
            "1", "Test", "Other", "Local", "Mix.",
            "salt", "to taste");

        RecipeCreateRequest req = TheMealDbMapper.toCreateRequest(meal, null);

        var ing = req.ingredients().get(0);
        assertThat(ing.amount()).isEqualByComparingTo("1");
        assertThat(ing.unit()).isEqualTo(Unit.PIECE);
        assertThat(ing.note()).isEqualTo("to taste");
    }

    @Test
    @DisplayName("Leere Zutatenliste wird zu Platzhalter-Eintrag")
    void emptyIngredientsBecomesPlaceholder() {
        TheMealDbMeal meal = meal("1", "Test", null, null, "Bake.");
        RecipeCreateRequest req = TheMealDbMapper.toCreateRequest(meal, null);

        assertThat(req.ingredients()).hasSize(1);
        assertThat(req.ingredients().get(0).ingredientName()).isEqualTo("Siehe Anleitung");
    }

    @Test
    @DisplayName("householdId wird durchgereicht")
    void householdIdPassedThrough() {
        UUID householdId = UUID.randomUUID();
        TheMealDbMeal meal = meal("1", "Test", "Other", "Local", "Mix.", "salt", "1g");

        RecipeCreateRequest req = TheMealDbMapper.toCreateRequest(meal, householdId);

        assertThat(req.householdId()).isEqualTo(householdId);
    }

    @Nested
    class MeasureParser {
        @Test
        @DisplayName("Bruch wird korrekt geparst")
        void fraction() {
            var p = TheMealDbMapper.parseMeasure("1/2 cup");
            assertThat(p.amount()).isEqualByComparingTo("0.5");
            assertThat(p.unit()).isEqualTo(Unit.PIECE);
            assertThat(p.fallback()).isFalse();
        }

        @Test
        @DisplayName("Komma als Dezimaltrennzeichen")
        void commaDecimal() {
            var p = TheMealDbMapper.parseMeasure("1,5 ml");
            assertThat(p.amount()).isEqualByComparingTo("1.5");
            assertThat(p.unit()).isEqualTo(Unit.ML);
        }

        @Test
        @DisplayName("Leeres Measure ist Fallback")
        void emptyIsFallback() {
            var p = TheMealDbMapper.parseMeasure("");
            assertThat(p.fallback()).isTrue();
            assertThat(p.amount()).isEqualByComparingTo("1");
            assertThat(p.unit()).isEqualTo(Unit.PIECE);
        }

        @Test
        @DisplayName("g/gram synonym")
        void gramSynonyms() {
            assertThat(TheMealDbMapper.parseMeasure("250 g").unit()).isEqualTo(Unit.GRAM);
            assertThat(TheMealDbMapper.parseMeasure("250 gram").unit()).isEqualTo(Unit.GRAM);
        }

        @Test
        @DisplayName("kg wird zu GRAM mit Menge x1000 skaliert (Bug-Fix)")
        void kilogramConvertsToGramWithScaledAmount() {
            var p = TheMealDbMapper.parseMeasure("2 kg");

            assertThat(p.unit()).isEqualTo(Unit.GRAM);
            assertThat(p.amount()).isEqualByComparingTo("2000");
            assertThat(p.fallback()).isFalse();
        }

        @Test
        @DisplayName("Liter wird zu ML mit Menge x1000 skaliert")
        void literConvertsToMlWithScaledAmount() {
            var p = TheMealDbMapper.parseMeasure("1.5 l");

            assertThat(p.unit()).isEqualTo(Unit.ML);
            assertThat(p.amount()).isEqualByComparingTo("1500.0");
            assertThat(p.fallback()).isFalse();
        }
    }

    // --- Builder ---------------------------------------------------------

    private static TheMealDbMeal meal(String id, String title, String category, String area,
                                      String instructions, String... ingredientPairs) {
        // Pairs sind (ingredient, measure)*. Wir füllen bis zu 20 Slots auf.
        String[] ing = new String[20];
        String[] meas = new String[20];
        for (int i = 0; i < ingredientPairs.length / 2 && i < 20; i++) {
            ing[i] = ingredientPairs[i * 2];
            meas[i] = ingredientPairs[i * 2 + 1];
        }
        return new TheMealDbMeal(
            id, title, category, area, instructions, null, null,
            ing[0], ing[1], ing[2], ing[3], ing[4], ing[5], ing[6], ing[7], ing[8], ing[9],
            ing[10], ing[11], ing[12], ing[13], ing[14], ing[15], ing[16], ing[17], ing[18], ing[19],
            meas[0], meas[1], meas[2], meas[3], meas[4], meas[5], meas[6], meas[7], meas[8], meas[9],
            meas[10], meas[11], meas[12], meas[13], meas[14], meas[15], meas[16], meas[17], meas[18], meas[19]);
    }
}
