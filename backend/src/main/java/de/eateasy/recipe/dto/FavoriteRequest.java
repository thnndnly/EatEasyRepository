package de.eateasy.recipe.dto;

import jakarta.validation.constraints.NotNull;

/** Body für PUT /recipes/{id}/favorite. */
public record FavoriteRequest(
    @NotNull Boolean favorite
) {
    public boolean favoriteValue() {
        return Boolean.TRUE.equals(favorite);
    }
}
