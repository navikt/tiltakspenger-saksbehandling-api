ALTER TABLE utbetalingsvedtak
    DROP CONSTRAINT meldekort_eller_behandling;

ALTER TABLE utbetalingsvedtak
    DROP COLUMN beregning_kilde;

ALTER TABLE utbetalingsvedtak
    ADD CONSTRAINT meldekort_eller_behandling_kilde
        CHECK (
            (meldekort_id IS NOT NULL AND behandling_id IS NULL) OR
            (behandling_id IS NOT NULL AND meldekort_id IS NULL)
            )
