ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS ventestatus jsonb DEFAULT NULL;

