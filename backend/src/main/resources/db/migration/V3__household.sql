-- Phase 2: Haushalt, Mitgliedschaft, Einladungen.
-- default_diet_tags: lockerer Vorfilter fuer Rezeptauswahl auf Haushaltsebene.
-- household_membership.role kapselt OWNER/MEMBER ohne separate Tabelle.

CREATE TABLE household (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL,
    default_diet_tags   VARCHAR(50)[] NOT NULL DEFAULT '{}',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_household_updated_at
    BEFORE UPDATE ON household
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE household_membership (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    household_id    UUID NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    joined_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, household_id)
);

CREATE INDEX idx_membership_user ON household_membership (user_id);
CREATE INDEX idx_membership_household ON household_membership (household_id);

CREATE TABLE household_invitation (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id    UUID NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    email           VARCHAR(255) NOT NULL,
    token           VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMP NOT NULL,
    accepted_at     TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invitation_token ON household_invitation (token);
CREATE INDEX idx_invitation_email ON household_invitation (LOWER(email));
