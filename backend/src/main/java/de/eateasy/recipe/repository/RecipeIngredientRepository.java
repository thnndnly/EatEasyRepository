package de.eateasy.recipe.repository;

import de.eateasy.recipe.entity.RecipeIngredient;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class RecipeIngredientRepository implements PanacheRepositoryBase<RecipeIngredient, UUID> {
}
