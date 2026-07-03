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

    /** Alle favorisierten Rezept-IDs eines Users — eine Query fuer die Listen-Anreicherung. */
    public Set<UUID> findRecipeIdsByUser(UUID userId) {
        return new HashSet<>(getEntityManager()
            .createQuery("select f.recipeId from RecipeFavorite f where f.userId = :userId", UUID.class)
            .setParameter("userId", userId)
            .getResultList());
    }
}
