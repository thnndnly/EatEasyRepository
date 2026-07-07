-- Phase 15: Rezept-Favoriten. Pro User (nicht pro Haushalt) — jedes Mitglied
-- pflegt seine eigene Favoritenliste. V9 statt V8, weil V8 bereits vom
-- parallelen Kategorien-Slice (feature/16) belegt ist.

CREATE TABLE user_favorite_recipe (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    recipe_id   UUID NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, recipe_id)
);

CREATE INDEX idx_user_favorite_recipe_user ON user_favorite_recipe (user_id);
CREATE INDEX idx_user_favorite_recipe_recipe ON user_favorite_recipe (recipe_id);
