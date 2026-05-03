package de.eateasy.common.diet;

import de.eateasy.common.exception.BadRequestException;

import java.util.List;
import java.util.Set;

/**
 * Whitelist gueltiger Diaet-Tags fuer Rezepte und Haushalts-Vorfilter.
 * Speicherung als Lower-Case-String, damit Postgres-Array-Spalten und Frontend
 * mit einem einzigen Vokabular arbeiten.
 */
public final class DietTag {

    public static final String VEGAN = "vegan";
    public static final String VEGETARIAN = "vegetarian";
    public static final String GLUTEN_FREE = "gluten_free";
    public static final String HALAL = "halal";
    public static final String LOW_CARB = "low_carb";
    public static final String DAIRY_FREE = "dairy_free";

    public static final Set<String> ALL = Set.of(
        VEGAN, VEGETARIAN, GLUTEN_FREE, HALAL, LOW_CARB, DAIRY_FREE);

    private DietTag() {
    }

    public static boolean isValid(String tag) {
        return tag != null && ALL.contains(tag);
    }

    /**
     * Validiert eine Liste von Diaet-Tags gegen die Whitelist und gibt eine
     * deduplizierte String-Reprasentation zurueck. Wirft
     * {@link BadRequestException} bei unbekanntem Tag.
     */
    public static String[] validate(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new String[0];
        }
        return tags.stream()
            .map(t -> {
                if (!isValid(t)) {
                    throw new BadRequestException("Unbekannter Diaet-Tag: " + t);
                }
                return t;
            })
            .distinct()
            .toArray(String[]::new);
    }
}
