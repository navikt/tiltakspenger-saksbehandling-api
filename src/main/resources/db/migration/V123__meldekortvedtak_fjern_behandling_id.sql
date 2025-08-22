ALTER TABLE meldekortvedtak
    DROP CONSTRAINT IF EXISTS meldekort_eller_behandling_kilde;

DELETE FROM meldekortvedtak WHERE id in (
    SELECT id from meldekortvedtak where behandling_id is not null order by opprettet
    );

ALTER TABLE meldekortvedtak
    DROP COLUMN behandling_id;

ALTER TABLE meldekortvedtak
    ALTER COLUMN meldekort_id SET NOT NULL;
