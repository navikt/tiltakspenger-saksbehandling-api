-- meldekortbehandling.beregninger
UPDATE meldekortbehandling
SET beregninger = replace(
    beregninger::text,
    '"FRAVÆR_VELFERD_GODKJENT_AV_NAV"',
    '"FRAVÆR_GODKJENT_AV_NAV"'
)::jsonb
WHERE beregninger::text LIKE '%FRAVÆR_VELFERD_GODKJENT_AV_NAV%';

UPDATE meldekortbehandling
SET beregninger = replace(
    beregninger::text,
    '"FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV"',
    '"FRAVÆR_ANNET"'
)::jsonb
WHERE beregninger::text LIKE '%FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV%';

UPDATE meldekortbehandling
SET beregninger = replace(
    beregninger::text,
    '"IKKE_UTFYLT"',
    '"IKKE_BESVART"'
)::jsonb
WHERE beregninger::text LIKE '%IKKE_UTFYLT%';

UPDATE meldekortbehandling
SET beregninger = replace(
    beregninger::text,
    '"IKKE_REGISTRERT"',
    '"IKKE_BESVART"'
)::jsonb
WHERE beregninger::text LIKE '%IKKE_REGISTRERT%';


-- meldekortbehandling.meldekortdager
UPDATE meldekortbehandling
SET meldekortdager = replace(
    meldekortdager::text,
    '"FRAVÆR_VELFERD_GODKJENT_AV_NAV"',
    '"FRAVÆR_GODKJENT_AV_NAV"'
)::jsonb
WHERE meldekortdager::text LIKE '%FRAVÆR_VELFERD_GODKJENT_AV_NAV%';

UPDATE meldekortbehandling
SET meldekortdager = replace(
    meldekortdager::text,
    '"FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV"',
    '"FRAVÆR_ANNET"'
)::jsonb
WHERE meldekortdager::text LIKE '%FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV%';

UPDATE meldekortbehandling
SET meldekortdager = replace(
    meldekortdager::text,
    '"IKKE_UTFYLT"',
    '"IKKE_BESVART"'
)::jsonb
WHERE meldekortdager::text LIKE '%IKKE_UTFYLT%';

UPDATE meldekortbehandling
SET meldekortdager = replace(
    meldekortdager::text,
    '"IKKE_REGISTRERT"',
    '"IKKE_BESVART"'
)::jsonb
WHERE meldekortdager::text LIKE '%IKKE_REGISTRERT%';


-- meldekortbruker.dager
UPDATE meldekort_bruker
SET dager = replace(
    dager::text,
    '"FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV"',
    '"FRAVÆR_ANNET"'
)::jsonb
WHERE dager::text LIKE '%FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV%';

UPDATE meldekort_bruker
SET dager = replace(
    dager::text,
    '"FRAVÆR_VELFERD_GODKJENT_AV_NAV"',
    '"FRAVÆR_GODKJENT_AV_NAV"'
)::jsonb
WHERE dager::text LIKE '%FRAVÆR_VELFERD_GODKJENT_AV_NAV%';

UPDATE meldekort_bruker
SET dager = replace(
    dager::text,
    '"IKKE_UTFYLT"',
    '"IKKE_BESVART"'
)::jsonb
WHERE dager::text LIKE '%IKKE_UTFYLT%';

UPDATE meldekort_bruker
SET dager = replace(
    dager::text,
    '"IKKE_REGISTRERT"',
    '"IKKE_BESVART"'
)::jsonb
WHERE dager::text LIKE '%IKKE_REGISTRERT%';
