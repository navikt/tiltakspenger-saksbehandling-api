ALTER TABLE meldekortvedtak
    DROP CONSTRAINT IF EXISTS meldekort_eller_behandling_kilde;

ALTER TABLE meldekortvedtak
    DROP COLUMN IF EXISTS behandling_id;

ALTER TABLE meldekortvedtak
    ALTER COLUMN meldekort_id SET NOT NULL;
