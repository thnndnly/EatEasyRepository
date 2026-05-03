package de.eateasy.ingredient.repository;

import de.eateasy.ingredient.entity.Ingredient;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IngredientRepository implements PanacheRepositoryBase<Ingredient, UUID> {

    public Optional<Ingredient> findByNameIgnoreCase(String name) {
        return find("LOWER(name) = ?1", name.toLowerCase()).firstResultOptional();
    }

    public List<Ingredient> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return list("id in ?1", ids);
    }

    /**
     * Volltext-Suche im Namen (case-insensitive). Liefert max. {@code limit}
     * Treffer sortiert nach Name. Bei leerem Query gibt es eine kurze
     * Vorschau-Liste (Top-N alphabetisch), damit der Picker initial nicht leer ist.
     */
    public List<Ingredient> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return findAll(Sort.by("name")).page(0, limit).list();
        }
        String pattern = "%" + query.toLowerCase() + "%";
        return find("LOWER(name) like ?1", Sort.by("name"), pattern)
            .page(0, limit)
            .list();
    }
}
