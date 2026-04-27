-- Phase 1: User-Tabelle fuer Auth.
-- "user" ist ein reserviertes Schluesselwort in Postgres → Tabelle heisst app_user.
-- Email wird in der Service-Schicht normalisiert (lowercase + trim) bevor sie persistiert
-- oder gesucht wird, damit case-insensitive Lookup funktioniert ohne CITEXT-Typ.

CREATE TABLE app_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email ON app_user (email);

CREATE TRIGGER trg_app_user_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
