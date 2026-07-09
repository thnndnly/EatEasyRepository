-- Soft-Delete fuer Rezepte (CLAUDE.md-Konvention "Soft-Delete Rezepte ja").
-- Geloeschte Rezepte bleiben als Zeile bestehen, damit bestehende Referenzen
-- (Wochenplan-Eintraege, Einkaufslisten, Favoriten) weiter aufloesen; sie
-- verschwinden aber aus Browse/Suche/Vorschlaegen.
-- NULL = aktiv, Zeitstempel = geloescht.
ALTER TABLE recipe ADD COLUMN deleted_at TIMESTAMP;

-- Beschleunigt das haeufige "deleted_at IS NULL"-Filtern in der Sichtbarkeits-Query.
CREATE INDEX idx_recipe_deleted_at ON recipe (deleted_at);
