package de.eateasy.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * TheMealDB liefert immer ein Wrapper-Objekt {@code {"meals": [...]}} oder
 * {@code {"meals": null}} bei keinem Treffer. Wir mappen nur die Felder, die
 * wir tatsächlich auswerten — alle anderen ignorieren wir explizit.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TheMealDbResponse(List<TheMealDbMeal> meals) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TheMealDbMeal(
        @JsonProperty("idMeal") String idMeal,
        @JsonProperty("strMeal") String strMeal,
        @JsonProperty("strCategory") String strCategory,
        @JsonProperty("strArea") String strArea,
        @JsonProperty("strInstructions") String strInstructions,
        @JsonProperty("strMealThumb") String strMealThumb,
        @JsonProperty("strSource") String strSource,
        @JsonProperty("strIngredient1") String strIngredient1,
        @JsonProperty("strIngredient2") String strIngredient2,
        @JsonProperty("strIngredient3") String strIngredient3,
        @JsonProperty("strIngredient4") String strIngredient4,
        @JsonProperty("strIngredient5") String strIngredient5,
        @JsonProperty("strIngredient6") String strIngredient6,
        @JsonProperty("strIngredient7") String strIngredient7,
        @JsonProperty("strIngredient8") String strIngredient8,
        @JsonProperty("strIngredient9") String strIngredient9,
        @JsonProperty("strIngredient10") String strIngredient10,
        @JsonProperty("strIngredient11") String strIngredient11,
        @JsonProperty("strIngredient12") String strIngredient12,
        @JsonProperty("strIngredient13") String strIngredient13,
        @JsonProperty("strIngredient14") String strIngredient14,
        @JsonProperty("strIngredient15") String strIngredient15,
        @JsonProperty("strIngredient16") String strIngredient16,
        @JsonProperty("strIngredient17") String strIngredient17,
        @JsonProperty("strIngredient18") String strIngredient18,
        @JsonProperty("strIngredient19") String strIngredient19,
        @JsonProperty("strIngredient20") String strIngredient20,
        @JsonProperty("strMeasure1") String strMeasure1,
        @JsonProperty("strMeasure2") String strMeasure2,
        @JsonProperty("strMeasure3") String strMeasure3,
        @JsonProperty("strMeasure4") String strMeasure4,
        @JsonProperty("strMeasure5") String strMeasure5,
        @JsonProperty("strMeasure6") String strMeasure6,
        @JsonProperty("strMeasure7") String strMeasure7,
        @JsonProperty("strMeasure8") String strMeasure8,
        @JsonProperty("strMeasure9") String strMeasure9,
        @JsonProperty("strMeasure10") String strMeasure10,
        @JsonProperty("strMeasure11") String strMeasure11,
        @JsonProperty("strMeasure12") String strMeasure12,
        @JsonProperty("strMeasure13") String strMeasure13,
        @JsonProperty("strMeasure14") String strMeasure14,
        @JsonProperty("strMeasure15") String strMeasure15,
        @JsonProperty("strMeasure16") String strMeasure16,
        @JsonProperty("strMeasure17") String strMeasure17,
        @JsonProperty("strMeasure18") String strMeasure18,
        @JsonProperty("strMeasure19") String strMeasure19,
        @JsonProperty("strMeasure20") String strMeasure20
    ) {

        /** Liefert die N-te Zutat als (name, measure) — null wenn leer/abwesend. */
        public IngredientSlot ingredient(int oneBasedIndex) {
            String name = byIndex(oneBasedIndex, true);
            String measure = byIndex(oneBasedIndex, false);
            if (name == null || name.isBlank()) {
                return null;
            }
            return new IngredientSlot(name.trim(), measure == null ? "" : measure.trim());
        }

        private String byIndex(int i, boolean ingredient) {
            return switch (i) {
                case 1 -> ingredient ? strIngredient1 : strMeasure1;
                case 2 -> ingredient ? strIngredient2 : strMeasure2;
                case 3 -> ingredient ? strIngredient3 : strMeasure3;
                case 4 -> ingredient ? strIngredient4 : strMeasure4;
                case 5 -> ingredient ? strIngredient5 : strMeasure5;
                case 6 -> ingredient ? strIngredient6 : strMeasure6;
                case 7 -> ingredient ? strIngredient7 : strMeasure7;
                case 8 -> ingredient ? strIngredient8 : strMeasure8;
                case 9 -> ingredient ? strIngredient9 : strMeasure9;
                case 10 -> ingredient ? strIngredient10 : strMeasure10;
                case 11 -> ingredient ? strIngredient11 : strMeasure11;
                case 12 -> ingredient ? strIngredient12 : strMeasure12;
                case 13 -> ingredient ? strIngredient13 : strMeasure13;
                case 14 -> ingredient ? strIngredient14 : strMeasure14;
                case 15 -> ingredient ? strIngredient15 : strMeasure15;
                case 16 -> ingredient ? strIngredient16 : strMeasure16;
                case 17 -> ingredient ? strIngredient17 : strMeasure17;
                case 18 -> ingredient ? strIngredient18 : strMeasure18;
                case 19 -> ingredient ? strIngredient19 : strMeasure19;
                case 20 -> ingredient ? strIngredient20 : strMeasure20;
                default -> null;
            };
        }
    }

    public record IngredientSlot(String name, String measure) {
    }
}
