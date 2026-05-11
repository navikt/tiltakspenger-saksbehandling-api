ALTER TABLE meldekortbehandling
    DROP COLUMN ikke_rett_til_tiltakspenger_tidspunkt;

-- Alle behandlinger med status IKKE_RETT_TIL_TILTAKSPENGER i db er avbrutte behandlinger
UPDATE meldekortbehandling
SET status = 'AVBRUTT'
WHERE status = 'IKKE_RETT_TIL_TILTAKSPENGER';
