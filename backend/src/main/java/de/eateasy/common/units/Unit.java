package de.eateasy.common.units;

/**
 * Maßeinheiten fuer Zutaten in Rezept, Vorrat und Einkaufsliste.
 * Persistierung als String (Enum-Name) — Mapping ueber {@link jakarta.persistence.EnumType#STRING}.
 *
 * <p>GRAM und ML sind die einzigen, die in Phase 6 (Einkaufsliste) konvertiert
 * werden — Stueckmengen (PIECE), Essloeffel (TBSP) und Teeloeffel (TSP) bleiben
 * unkonvertiert in der Aggregation.</p>
 */
public enum Unit {
    GRAM,
    ML,
    PIECE,
    TBSP,
    TSP
}
