package de.eateasy.integration.service;

import de.eateasy.common.diet.DietTag;
import de.eateasy.common.units.Unit;
import de.eateasy.integration.client.TheMealDbResponse.IngredientSlot;
import de.eateasy.integration.client.TheMealDbResponse.TheMealDbMeal;
import de.eateasy.recipe.dto.RecipeCreateRequest;
import de.eateasy.recipe.dto.RecipeIngredientRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wandelt eine TheMealDB-Antwort in einen {@link RecipeCreateRequest} um. Pure
 * Funktion (keine CDI-Abhaengigkeiten), damit der Mapping-Code als reiner
 * Unit-Test ohne Quarkus-Boot pruefbar bleibt.
 *
 * <p>TheMealDB liefert keine Portionsangabe und keine Zubereitungszeit —
 * Default {@link #DEFAULT_SERVINGS}. Mengen werden grob aus {@code strMeasure}
 * geparst (z. B. "200g", "1 cup", "1/2 tsp"). Was sich nicht parsen laesst,
 * landet als 1 PIECE mit der Original-Einheit als note.</p>
 */
public final class TheMealDbMapper {

    private static final int DEFAULT_SERVINGS = 4;

    /** "200g", "1.5 ml", "1/2 tsp", " 2 tbsp " — alles, was wir hinkriegen. */
    private static final Pattern AMOUNT_UNIT = Pattern.compile(
        "^\\s*(?<num>\\d+(?:[\\.,]\\d+)?|\\d+\\s*/\\s*\\d+)\\s*(?<unit>[a-zA-Z]+)?\\s*$");

    private TheMealDbMapper() {
    }

    public static RecipeCreateRequest toCreateRequest(TheMealDbMeal meal, UUID householdId) {
        if (meal == null) {
            throw new IllegalArgumentException("meal must not be null");
        }
        return new RecipeCreateRequest(
            safeTrim(meal.strMeal(), "Importiertes Rezept"),
            buildDescription(meal),
            safeTrim(meal.strInstructions(), "Keine Anleitung verfuegbar."),
            DEFAULT_SERVINGS,
            null,
            deriveDietTags(meal.strCategory()),
            householdId,
            mapIngredients(meal));
    }

    private static String safeTrim(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String buildDescription(TheMealDbMeal meal) {
        StringBuilder sb = new StringBuilder();
        if (meal.strCategory() != null && !meal.strCategory().isBlank()) {
            sb.append(meal.strCategory().trim());
        }
        if (meal.strArea() != null && !meal.strArea().isBlank()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(meal.strArea().trim());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static List<String> deriveDietTags(String category) {
        if (category == null) {
            return List.of();
        }
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "vegan" -> List.of(DietTag.VEGAN, DietTag.VEGETARIAN, DietTag.DAIRY_FREE);
            case "vegetarian" -> List.of(DietTag.VEGETARIAN);
            default -> List.of();
        };
    }

    private static List<RecipeIngredientRequest> mapIngredients(TheMealDbMeal meal) {
        List<RecipeIngredientRequest> out = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            IngredientSlot slot = meal.ingredient(i);
            if (slot == null) {
                continue;
            }
            ParsedMeasure parsed = parseMeasure(slot.measure());
            String note = parsed.fallback ? slot.measure() : null;
            out.add(new RecipeIngredientRequest(
                null,
                slot.name(),
                parsed.amount,
                parsed.unit,
                note == null || note.isBlank() ? null : note));
        }
        if (out.isEmpty()) {
            // RecipeService verlangt mindestens eine Zutat — wir geben einen
            // Platzhalter aus, damit der Import nicht hart fehlschlaegt.
            out.add(new RecipeIngredientRequest(
                null, "Siehe Anleitung", BigDecimal.ONE, Unit.PIECE, null));
        }
        return out;
    }

    static ParsedMeasure parseMeasure(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedMeasure(BigDecimal.ONE, Unit.PIECE, true);
        }
        Matcher m = AMOUNT_UNIT.matcher(raw);
        if (!m.matches()) {
            return new ParsedMeasure(BigDecimal.ONE, Unit.PIECE, true);
        }
        BigDecimal amount = parseFraction(m.group("num"));
        String unitToken = m.group("unit");
        Unit unit = mapUnit(unitToken);
        return new ParsedMeasure(amount, unit, false);
    }

    private static BigDecimal parseFraction(String token) {
        token = token.replace(',', '.').replaceAll("\\s+", "");
        if (token.contains("/")) {
            String[] parts = token.split("/");
            BigDecimal num = new BigDecimal(parts[0]);
            BigDecimal den = new BigDecimal(parts[1]);
            return num.divide(den, 4, java.math.RoundingMode.HALF_UP);
        }
        return new BigDecimal(token);
    }

    private static Unit mapUnit(String token) {
        if (token == null) {
            return Unit.PIECE;
        }
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "g", "gr", "gram", "grams", "gramme", "gramms" -> Unit.GRAM;
            case "kg" -> Unit.GRAM; // Konvertierung kommt erst in Stretch — vorerst as-is.
            case "ml", "milliliter", "milliliters" -> Unit.ML;
            case "l", "liter", "liters", "litre", "litres" -> Unit.ML;
            case "tbsp", "tbs", "tablespoon", "tablespoons", "el" -> Unit.TBSP;
            case "tsp", "teaspoon", "teaspoons", "tl" -> Unit.TSP;
            default -> Unit.PIECE;
        };
    }

    record ParsedMeasure(BigDecimal amount, Unit unit, boolean fallback) {
    }
}
