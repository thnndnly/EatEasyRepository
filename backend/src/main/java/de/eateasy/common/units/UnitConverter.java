package de.eateasy.common.units;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Konvertiert Mengen zwischen verwandten Einheiten auf eine kanonische
 * Basis-Einheit. Aktuell unterstützt:
 *
 * <ul>
 *   <li>{@link Unit#TBSP} (1 EL = 15 ml) und {@link Unit#TSP} (1 TL = 5 ml) →
 *       Basis {@link Unit#ML}</li>
 *   <li>{@link Unit#GRAM}, {@link Unit#ML}, {@link Unit#PIECE} bleiben
 *       unverändert (eigene Basis).</li>
 * </ul>
 *
 * <p>Wird im {@code ShoppingListService} genutzt, damit ein Rezept mit
 * „2 EL Olivenöl" und ein anderes mit „30 ml Olivenöl" auf einer einzigen
 * Liste-Zeile zusammenfallen. Auch der Pantry-Abzug normalisiert vorab.</p>
 */
public final class UnitConverter {

    private static final Map<Unit, Unit> CANONICAL = Map.of(
        Unit.GRAM, Unit.GRAM,
        Unit.ML, Unit.ML,
        Unit.PIECE, Unit.PIECE,
        Unit.TBSP, Unit.ML,
        Unit.TSP, Unit.ML
    );

    private static final Map<Unit, BigDecimal> FACTOR_TO_CANONICAL = Map.of(
        Unit.GRAM, BigDecimal.ONE,
        Unit.ML, BigDecimal.ONE,
        Unit.PIECE, BigDecimal.ONE,
        Unit.TBSP, new BigDecimal("15"),
        Unit.TSP, new BigDecimal("5")
    );

    private UnitConverter() {
    }

    /** Liefert die Basis-Einheit für die übergebene Einheit. */
    public static Unit canonical(Unit unit) {
        return CANONICAL.getOrDefault(unit, unit);
    }

    /**
     * Wandelt einen Betrag in die kanonische Einheit. Beispiel:
     * {@code toCanonical(BigDecimal.valueOf(2), Unit.TBSP) == 30 ml}.
     */
    public static BigDecimal toCanonical(BigDecimal amount, Unit unit) {
        BigDecimal factor = FACTOR_TO_CANONICAL.getOrDefault(unit, BigDecimal.ONE);
        return amount.multiply(factor);
    }
}
