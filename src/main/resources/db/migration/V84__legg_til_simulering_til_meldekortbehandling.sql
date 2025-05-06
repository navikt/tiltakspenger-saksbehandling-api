ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS simulering JSONB;