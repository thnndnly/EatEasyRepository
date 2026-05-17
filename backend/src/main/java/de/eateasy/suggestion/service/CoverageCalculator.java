package de.eateasy.suggestion.service;

import de.eateasy.recipe.dto.RecipeIngredientView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pure Funktion: berechnet pro Rezept den Vorrats-Abdeckungsgrad —
 * {@code matched / total} der Zutaten, die der Haushalt zumindest in
 * irgendeiner Menge im Vorrat hat. Die exakte Mengen-Pruefung uebernimmt
 * spaeter die Einkaufsliste; fuer die Suggestion-Heuristik reicht die
 * Anwesenheit der Zutat.
 *
 * <p>Rezepte ohne Zutaten erhalten Coverage {@code 0} (kein Match moeglich).</p>
 */
public final class CoverageCalculator {

    private CoverageCalculator() {
    }

    public static Map<UUID, Double> compute(
        Map<UUID, List<RecipeIngredientView>> ingredientsByRecipe,
        Set<UUID> pantryIngredientIds
    ) {
        Map<UUID, Double> result = new HashMap<>();
        for (Map.Entry<UUID, List<RecipeIngredientView>> entry : ingredientsByRecipe.entrySet()) {
            List<RecipeIngredientView> ingredients = entry.getValue();
            if (ingredients == null || ingredients.isEmpty()) {
                result.put(entry.getKey(), 0.0);
                continue;
            }
            int matched = 0;
            for (RecipeIngredientView ing : ingredients) {
                if (pantryIngredientIds.contains(ing.ingredientId())) {
                    matched++;
                }
            }
            result.put(entry.getKey(), (double) matched / ingredients.size());
        }
        return result;
    }
}
