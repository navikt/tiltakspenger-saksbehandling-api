-- Endrer utbetaling_id (VARCHAR) til utbetaling_ider (TEXT[]) for å støtte at en
-- tilbakekrevingsbehandling kan referere til flere utbetalinger.
-- Vi mister fremmednøkkel-referansen til utbetaling(id), siden Postgres ikke støtter
-- FK på array-elementer. Konsistens må håndteres i applikasjonslaget.

ALTER TABLE tilbakekreving_behandling
    ADD COLUMN utbetaling_ider TEXT[] NOT NULL DEFAULT '{}';

UPDATE tilbakekreving_behandling
SET utbetaling_ider = ARRAY[utbetaling_id]
WHERE utbetaling_id IS NOT NULL;

ALTER TABLE tilbakekreving_behandling
    ALTER COLUMN utbetaling_ider DROP DEFAULT,
    ADD CONSTRAINT tilbakekreving_behandling_utbetaling_ider_not_empty
        CHECK (cardinality(utbetaling_ider) > 0);

DROP INDEX IF EXISTS idx_tilbakekreving_behandling_utbetaling_id;

CREATE INDEX idx_tilbakekreving_behandling_utbetaling_ider
    ON tilbakekreving_behandling USING GIN (utbetaling_ider);

ALTER TABLE tilbakekreving_behandling DROP COLUMN utbetaling_id;

