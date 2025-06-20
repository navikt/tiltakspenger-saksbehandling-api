UPDATE meldekortbehandling
SET beregninger = replace(
    beregninger::text,
    '"SPERRET"',
    '"IKKE_RETT_TIL_TILTAKSPENGER"'
)::jsonb
WHERE beregninger::text LIKE '%SPERRET%';

UPDATE meldekortbehandling
SET meldekortdager = replace(
    meldekortdager::text,
    '"SPERRET"',
    '"IKKE_RETT_TIL_TILTAKSPENGER"'
)::jsonb
WHERE meldekortdager::text LIKE '%SPERRET%';
