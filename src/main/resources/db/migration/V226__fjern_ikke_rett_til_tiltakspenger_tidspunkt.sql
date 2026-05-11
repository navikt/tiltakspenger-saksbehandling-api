ALTER TABLE meldekortbehandling
    DROP COLUMN ikke_rett_til_tiltakspenger_tidspunkt;

UPDATE meldekortbehandling
SET status = 'AVBRUTT'
WHERE status = 'IKKE_RETT_TIL_TILTAKSPENGER';
