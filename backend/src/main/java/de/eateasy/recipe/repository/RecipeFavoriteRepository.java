package de.eateasy.recipe.repository;

import de.eateasy.recipe.entity.RecipeFavorite;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class RecipeFavoriteRepository implements PanacheRepositoryBase<RecipeFavorite, UUID> {

    public Optional<RecipeFavorite> findByUserAndRecipe(UUID userId, UUID recipeId) {
        return find("userId = ?1 and recipeId = ?2", userId, recipeId).firstResultOptional();
    }

    /**
     * Legt einen Favoriten idempotent an. {@code INSERT ... ON CONFLICT DO NOTHING}
     * ist auf DB-Ebene atomar und schuetzt gegen die TOCTOU-Race zwischen
     * {@link #findByUserAndRecipe} und {@code persist}: zwei parallele
     * PUT .../favorite (Doppelklick, zwei Tabs) koennen nicht beide inserten und
     * loesen keinen Unique-Constraint-Fehler (UNIQUE(user_id, recipe_id)) aus.
     *
     * @return {@code true}, wenn eine neue Zeile angelegt wurde; {@code false}, wenn der Favorit bereits bestand.
     */
    public boolean insertIfAbsent(UUID userId, UUID recipeId) {
        int inserted = getEntityManager()
            .createNativeQuery("insert into user_favorite_recipe (id, user_id, recipe_id, created_at) "
                + "values (?1, ?2, ?3, current_timestamp) on conflict (user_id, recipe_id) do nothing")
            .setParameter(1, UUID.randomUUID())
            .setParameter(2, userId)
            .setParameter(3, recipeId)
            .executeUpdate();
        return inserted > 0;
    }

    /** Alle favorisierten Rezept-IDs eines Users — eine Query fuer die Listen-Anreicherung. */
    public Set<UUID> findRecipeIdsByUser(UUID userId) {
        return new HashSet<>(getEntityManager()
            .createQuery("select f.recipeId from RecipeFavorite f where f.userId = :userId", UUID.class)
            .setParameter("userId", userId)
            .getResultList());
    }
}
