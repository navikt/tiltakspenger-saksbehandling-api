ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS skal_sende_vedtaksbrev boolean default true;