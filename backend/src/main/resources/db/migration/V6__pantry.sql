-- Phase 5: Vorratskammer pro Haushalt.
-- Mehrere Zeilen pro Zutat sind erlaubt (z. B. wenn unterschiedliche
-- Einheiten verwendet werden) — der Service aggregiert beim Hinzufuegen
-- nur, wenn dieselbe Unit schon existiert.

CREATE TABLE pantry_item (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id    UUID NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    ingredient_id   UUID NOT NULL REFERENCES ingredient(id),
    amount          DECIMAL(10,2) NOT NULL,
    unit            VARCHAR(20) NOT NULL,
    best_before     DATE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pantry_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_pantry_household ON pantry_item (household_id);
CREATE INDEX idx_pantry_ingredient ON pantry_item (ingredient_id);

CREATE TRIGGER trg_pantry_updated_at
    BEFORE UPDATE ON pantry_item
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
