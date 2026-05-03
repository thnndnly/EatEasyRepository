-- Phase 6: Einkaufsliste, lazy berechnet aus Wochenplan minus Vorrat.
-- shopping_list ist pro (Haushalt, Wochenplan) eindeutig — wir generieren
-- maximal eine Liste pro Plan. Bei Regenerate wird die Liste umgebaut, der
-- Datensatz selbst aber wiederverwendet, sodass {@code checked}-Status fuer
-- gleiche (ingredient_id, unit)-Kombinationen erhalten bleibt.

CREATE TABLE shopping_list (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id    UUID NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    meal_plan_id    UUID NOT NULL REFERENCES meal_plan(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(household_id, meal_plan_id)
);

CREATE INDEX idx_shopping_list_household ON shopping_list (household_id);

CREATE TRIGGER trg_shopping_list_updated_at
    BEFORE UPDATE ON shopping_list
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE shopping_list_item (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shopping_list_id    UUID NOT NULL REFERENCES shopping_list(id) ON DELETE CASCADE,
    ingredient_id       UUID NOT NULL REFERENCES ingredient(id),
    amount              DECIMAL(10,2) NOT NULL,
    unit                VARCHAR(20) NOT NULL,
    checked             BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_shopping_item_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_shopping_item_list ON shopping_list_item (shopping_list_id);
