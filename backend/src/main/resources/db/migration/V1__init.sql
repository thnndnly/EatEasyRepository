-- EatEasy EE — Flyway-Baseline
-- Postgres 16 hat gen_random_uuid() out of the box (kein pgcrypto noetig).
-- Diese Migration legt nur Extensions / Defaults an, die spaetere Phasen voraussetzen duerfen.

-- citext fuer case-insensitive Indizes (z. B. Email-Lookup in Phase 1)
CREATE EXTENSION IF NOT EXISTS citext;

-- Hilfs-Funktion: setzt updated_at automatisch beim UPDATE.
-- Wird ab Phase 1 von jeder neuen Tabelle via Trigger genutzt.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
