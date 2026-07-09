package de.eateasy.common.units;

import java.util.Locale;

/**
 * Zentraler Parser für Einheits-Tokens aus externen Quellen (TheMealDB,
 * OpenFoodFacts). Liefert immer ein Paar aus {@link Unit} und Multiplikator —
 * letzterer ist {@code 1.0} für Einheiten, die schon in der kanonischen
 * Form vorliegen, und {@code 1000.0} für {@code kg} → {@code g} bzw.
 * {@code l} → {@code ml}.
 *
 * <p>Damit ersetzt diese Klasse das fehleranfällige Pattern, in dem
 * {@code kg} stillschweigend als {@link Unit#GRAM} mit gleichem Wert
 * interpretiert wurde — der Multiplikator muss vom Aufrufer auf die
 * Menge angewendet werden.</p>
 */
public final class UnitParser {

    /** Ergebnis-Record: {@link Unit} und Multiplikator, mit dem die Menge skaliert werden muss. */
    public record UnitParseResult(Unit unit, double multiplier) {
    }

    private UnitParser() {
    }

    /**
     * Parst ein Einheits-Token. {@code null}/leer führt zum {@code defaultUnit}
     * mit Multiplier {@code 1.0}; unbekannte Tokens ebenfalls.
     *
     * @param token       das Roh-Token, z. B. {@code "kg"}, {@code "EL"}
     * @param defaultUnit Fallback, wenn das Token leer oder unbekannt ist
     */
    public static UnitParseResult parse(String token, Unit defaultUnit) {
        if (token == null || token.isBlank()) {
            return new UnitParseResult(defaultUnit, 1.0);
        }
        return switch (token.trim().toLowerCase(Locale.ROOT)) {
            case "g", "gr", "gram", "grams", "gramme", "gramms" ->
                new UnitParseResult(Unit.GRAM, 1.0);
            case "kg", "kilogram", "kilograms" ->
                new UnitParseResult(Unit.GRAM, 1000.0);
            case "ml", "milliliter", "milliliters" ->
                new UnitParseResult(Unit.ML, 1.0);
            case "l", "liter", "liters", "litre", "litres" ->
                new UnitParseResult(Unit.ML, 1000.0);
            case "tbsp", "tbs", "tablespoon", "tablespoons", "el" ->
                new UnitParseResult(Unit.TBSP, 1.0);
            case "tsp", "teaspoon", "teaspoons", "tl" ->
                new UnitParseResult(Unit.TSP, 1.0);
            case "piece", "pieces", "pcs", "stück", "stück", "st" ->
                new UnitParseResult(Unit.PIECE, 1.0);
            default ->
                new UnitParseResult(defaultUnit, 1.0);
        };
    }
}
