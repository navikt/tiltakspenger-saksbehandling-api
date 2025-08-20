ALTER TABLE meldekortvedtak
    DROP CONSTRAINT meldekort_eller_behandling_kilde;

ALTER TABLE meldekortvedtak
    DROP COLUMN behandling_id;

ALTER TABLE meldekortvedtak
    ALTER COLUMN meldekort_id SET NOT NULL;
