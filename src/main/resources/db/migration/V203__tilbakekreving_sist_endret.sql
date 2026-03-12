ALTER TABLE tilbakekreving_behandling
    ADD COLUMN IF NOT EXISTS sist_endret TIMESTAMPTZ;

UPDATE tilbakekreving_behandling
SET sist_endret = opprettet
WHERE sist_endret IS NULL;

ALTER TABLE tilbakekreving_behandling
    ALTER COLUMN sist_endret SET NOT NULL;
