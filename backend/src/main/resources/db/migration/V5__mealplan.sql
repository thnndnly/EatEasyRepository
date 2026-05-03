-- Phase 4: Wochenplan pro Haushalt mit Slots fuer Tag x Mahlzeit.
-- meal_plan ist pro (haushalt, kalenderwoche) eindeutig — week_start ist
--   immer der Montag, Normalisierung passiert im Service.
-- meal_plan_entry hat Unique-Constraint auf (plan, tag, mahlzeit), damit
--   PUT als upsert funktioniert.

CREATE TABLE meal_plan (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id    UUID NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    week_start      DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(household_id, week_start)
);

CREATE INDEX idx_meal_plan_household ON meal_plan (household_id);

CREATE TRIGGER trg_meal_plan_updated_at
    BEFORE UPDATE ON meal_plan
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE meal_plan_entry (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_plan_id    UUID NOT NULL REFERENCES meal_plan(id) ON DELETE CASCADE,
    day_of_week     VARCHAR(10) NOT NULL,
    meal_type       VARCHAR(20) NOT NULL,
    recipe_id       UUID NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
    servings        INT NOT NULL,
    UNIQUE(meal_plan_id, day_of_week, meal_type),
    CONSTRAINT chk_meal_plan_entry_servings_positive CHECK (servings > 0)
);

CREATE INDEX idx_meal_plan_entry_plan ON meal_plan_entry (meal_plan_id);
CREATE INDEX idx_meal_plan_entry_recipe ON meal_plan_entry (recipe_id);
