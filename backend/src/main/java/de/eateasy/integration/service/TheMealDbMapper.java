package de.eateasy.integration.service;

import de.eateasy.common.diet.DietTag;
import de.eateasy.common.units.Unit;
import de.eateasy.common.units.UnitParser;
import de.eateasy.common.units.UnitParser.UnitParseResult;
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
 * Funktion (keine CDI-Abhängigkeiten), damit der Mapping-Code als reiner
 * Unit-Test ohne Quarkus-Boot prüfbar bleibt.
 *
 * <p>TheMealDB liefert keine Portionsangabe und keine Zubereitungszeit —
 * Default {@link #DEFAULT_SERVINGS}. Mengen werden grob aus {@code strMeasure}
 * geparst (z. B. "200g", "1 cup", "1/2 tsp"). Was sich nicht parsen lässt,
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
            safeTrim(meal.strInstructions(), "Keine Anleitung verfügbar."),
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
            // Platzhalter aus, damit der Import nicht hart fehlschlägt.
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
        BigDecimal amount;
        try {
            amount = parseFraction(m.group("num"));
        } catch (ArithmeticException | NumberFormatException ex) {
            // Unerwartete externe Mengenangabe (z. B. "1/0") — kontrollierter
            // Fallback statt 500, analog zum nicht-matchenden Fall oben.
            return new ParsedMeasure(BigDecimal.ONE, Unit.PIECE, true);
        }
        UnitParseResult unitResult = UnitParser.parse(m.group("unit"), Unit.PIECE);
        // Multiplier != 1 entspricht einer Einheitskonvertierung (z. B. kg → g).
        // Wir wenden ihn direkt auf die Menge an, damit das DTO konsistent in
        // kanonischen Einheiten landet.
        if (unitResult.multiplier() != 1.0) {
            amount = amount.multiply(BigDecimal.valueOf(unitResult.multiplier()));
        }
        return new ParsedMeasure(amount, unitResult.unit(), false);
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

    record ParsedMeasure(BigDecimal amount, Unit unit, boolean fallback) {
    }
}
