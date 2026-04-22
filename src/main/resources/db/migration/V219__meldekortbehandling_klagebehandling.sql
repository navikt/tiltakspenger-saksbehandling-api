ALTER TABLE meldekortbehandling ADD COLUMN IF NOT EXISTS klagebehandling_id varchar REFERENCES klagebehandling(id);
