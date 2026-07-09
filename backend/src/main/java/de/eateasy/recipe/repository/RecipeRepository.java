package de.eateasy.recipe.repository;

import de.eateasy.recipe.entity.Recipe;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RecipeRepository implements PanacheRepositoryBase<Recipe, UUID> {

    public List<Recipe> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return list("id in ?1", ids);
    }

    /**
     * Liefert Rezepte, die für einen User sichtbar sind:
     * <ul>
     *   <li>{@code ownerId == userId} (eigenes, ggf. privates Rezept), ODER</li>
     *   <li>{@code householdId} ist einer der Haushalte des Users.</li>
     * </ul>
     * Optionale Filter: Volltextsuche im Titel, Diät-Tag-AND-Match,
     * konkreter Haushalt.
     */
    public List<Recipe> search(UUID userId,
                               Collection<UUID> householdIds,
                               String query,
                               Collection<String> dietTags,
                               UUID householdFilter) {
        StringBuilder hql = new StringBuilder("from Recipe r where (r.ownerId = :userId");
        Parameters params = Parameters.with("userId", userId);

        // Sichtbarkeit: own OR (householdId in :hhs).
        if (householdIds != null && !householdIds.isEmpty()) {
            hql.append(" or r.householdId in :hhs");
            params = params.and("hhs", householdIds);
        }
        hql.append(")");

        // Soft-Delete: gelöschte Rezepte tauchen nicht in Browse/Suche/Vorschlägen
        // auf. Referenz-Auflösung (findByIds) bleibt bewusst ungefiltert.
        hql.append(" and r.deletedAt is null");

        if (householdFilter != null) {
            hql.append(" and r.householdId = :hhFilter");
            params = params.and("hhFilter", householdFilter);
        }

        if (query != null && !query.isBlank()) {
            hql.append(" and lower(r.title) like :q");
            params = params.and("q", "%" + query.toLowerCase() + "%");
        }

        // Diät-Tags AND-Match: jeder gesuchte Tag muss im Array enthalten sein.
        // Wir filtern die Tags nach dem Query in Java (post-filter, siehe unten)
        // statt per Postgres-Array-Operator — einfacher und für die Datenmengen
        // in diesem Studienprojekt völlig ausreichend.

        List<Recipe> raw = find(hql.toString(), Sort.by("title"), params).list();

        if (dietTags == null || dietTags.isEmpty()) {
            return raw;
        }
        List<Recipe> filtered = new ArrayList<>(raw.size());
        for (Recipe r : raw) {
            String[] tags = r.getDietTags();
            boolean allMatch = true;
            for (String wanted : dietTags) {
                boolean found = false;
                for (String present : tags) {
                    if (present.equalsIgnoreCase(wanted)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                filtered.add(r);
            }
        }
        return filtered;
    }
}
