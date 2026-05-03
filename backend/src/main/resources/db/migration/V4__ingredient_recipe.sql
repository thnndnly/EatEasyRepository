-- Phase 3: Globale Zutaten + Rezepte mit Zutatenliste und Diaet-Tags.
-- ingredient ist eine global geteilte Tabelle (alle User sehen denselben Pool),
--   Lookup case-insensitive ueber den functional index.
-- recipe.household_id NULL = privates Rezept; sonst sichtbar fuer alle Mitglieder.
-- recipe_ingredient ist die Junction; ON DELETE CASCADE auf recipe.

CREATE TABLE ingredient (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    default_unit    VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_ingredient_name_lower ON ingredient (LOWER(name));

CREATE TRIGGER trg_ingredient_updated_at
    BEFORE UPDATE ON ingredient
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE recipe (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id            UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    household_id        UUID REFERENCES household(id) ON DELETE SET NULL,
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    instructions        TEXT NOT NULL,
    servings            INT NOT NULL DEFAULT 2,
    prep_minutes        INT,
    diet_tags           VARCHAR(50)[] NOT NULL DEFAULT '{}',
    source_url          VARCHAR(500),
    external_source     VARCHAR(50),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_recipe_servings_positive CHECK (servings > 0),
    CONSTRAINT chk_recipe_prep_minutes_nonneg CHECK (prep_minutes IS NULL OR prep_minutes >= 0)
);

CREATE INDEX idx_recipe_owner ON recipe (owner_id);
CREATE INDEX idx_recipe_household ON recipe (household_id);
CREATE INDEX idx_recipe_title_lower ON recipe (LOWER(title));

CREATE TRIGGER trg_recipe_updated_at
    BEFORE UPDATE ON recipe
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE recipe_ingredient (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id       UUID NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
    ingredient_id   UUID NOT NULL REFERENCES ingredient(id),
    amount          DECIMAL(10,2) NOT NULL,
    unit            VARCHAR(20) NOT NULL,
    note            VARCHAR(200),
    CONSTRAINT chk_recipe_ingredient_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_recipe_ingredient_recipe ON recipe_ingredient (recipe_id);
CREATE INDEX idx_recipe_ingredient_ingredient ON recipe_ingredient (ingredient_id);
