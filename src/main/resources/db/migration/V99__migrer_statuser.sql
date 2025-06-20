UPDATE meldekortbehandling
SET beregninger = replace(
    beregninger::text,
    '"IKKE_DELTATT"',
    '"IKKE_TILTAKSDAG"'
)::jsonb
WHERE beregninger::text LIKE '%IKKE_DELTATT%';

UPDATE meldekortbehandling
SET meldekortdager = replace(
    meldekortdager::text,
    '"IKKE_DELTATT"',
    '"IKKE_TILTAKSDAG"'
)::jsonb
WHERE meldekortdager::text LIKE '%IKKE_DELTATT%';
