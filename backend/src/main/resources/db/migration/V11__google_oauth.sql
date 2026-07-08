-- Phase 12: Google OAuth.
-- Google-only-User haben kein Passwort → password_hash wird optional.
-- google_sub (Google "subject") identifiziert/verknuepft den Google-Account.
ALTER TABLE app_user ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE app_user ADD COLUMN google_sub varchar(255) UNIQUE;
