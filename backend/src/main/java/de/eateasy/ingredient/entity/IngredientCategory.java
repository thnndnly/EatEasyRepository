package de.eateasy.ingredient.entity;

/**
 * Supermarkt-Kategorie einer Zutat. Wird fuer die Gruppierung der
 * Einkaufsliste genutzt (Phase 16) — die Reihenfolge hier entspricht einem
 * typischen Gang durch den Supermarkt und wird vom Frontend gespiegelt.
 */
public enum IngredientCategory {
    OBST_GEMUESE,
    BACKWAREN,
    MILCHPRODUKTE,
    FLEISCH_FISCH,
    VORRAT,
    GEWUERZE_SAUCEN,
    TIEFKUEHL,
    GETRAENKE,
    SONSTIGES
}
