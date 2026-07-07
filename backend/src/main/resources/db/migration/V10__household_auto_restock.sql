-- Phase 14: Auto-Nachbuchen per Haushalt an-/abschaltbar.
-- Default true erhaelt das bisherige (bedingungslose) Verhalten fuer alle
-- Bestands-Haushalte.
ALTER TABLE household
    ADD COLUMN auto_restock_enabled boolean NOT NULL DEFAULT true;
