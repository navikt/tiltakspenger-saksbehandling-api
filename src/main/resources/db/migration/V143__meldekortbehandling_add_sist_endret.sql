ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS sist_endret timestamptz;

UPDATE meldekortbehandling
SET sist_endret = iverksatt_tidspunkt;

UPDATE meldekortbehandling
SET sist_endret = opprettet
WHERE sist_endret IS NULL;

ALTER TABLE meldekortbehandling
    ALTER COLUMN sist_endret SET NOT NULL;