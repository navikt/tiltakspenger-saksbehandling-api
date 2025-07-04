ALTER TABLE utbetalingsvedtak
    ADD COLUMN IF NOT EXISTS beregning_kilde VARCHAR NOT NULL DEFAULT 'MELDEKORT';

ALTER TABLE utbetalingsvedtak
    ADD COLUMN IF NOT EXISTS behandling_id VARCHAR REFERENCES behandling(id);

ALTER TABLE utbetalingsvedtak
    ALTER COLUMN meldekort_id DROP NOT NULL;

ALTER TABLE utbetalingsvedtak
    ADD CONSTRAINT meldekort_eller_behandling
        CHECK (
            (beregning_kilde = 'MELDEKORT' AND meldekort_id IS NOT NULL AND behandling_id IS NULL) OR
            (beregning_kilde = 'BEHANDLING' AND behandling_id IS NOT NULL AND meldekort_id IS NULL)
            )
