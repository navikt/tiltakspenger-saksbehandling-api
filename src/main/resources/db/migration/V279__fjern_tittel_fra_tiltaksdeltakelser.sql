-- Tittelen på tiltaket kan inneholde opplysninger vi ikke ønsker å lagre, og slås derfor opp hos kilden ved behov.
-- Her fjernes titler som allerede er lagret i behandlingenes saksopplysninger og innvilgelsesperioder.
-- Rekkefølgen i listene beholdes, og øvrige felter kopieres uendret.
-- En innvilgelsesperiode uten tiltaksdeltakelser beholder den tomme listen.
UPDATE behandling
SET saksopplysninger = jsonb_set(
    saksopplysninger,
    '{tiltaksdeltagelse}',
    (
        SELECT jsonb_agg(deltakelse - 'tittel' ORDER BY rekkefølge)
        FROM jsonb_array_elements(saksopplysninger -> 'tiltaksdeltagelse') WITH ORDINALITY AS deltakelser(deltakelse, rekkefølge)
    )
)
WHERE jsonb_path_exists(saksopplysninger, '$.tiltaksdeltagelse[*].tittel');

UPDATE behandling
SET saksopplysninger = jsonb_set(
    saksopplysninger,
    '{tiltaksdeltakelserSomFratarRett}',
    (
        SELECT jsonb_agg(deltakelse - 'tittel' ORDER BY rekkefølge)
        FROM jsonb_array_elements(saksopplysninger -> 'tiltaksdeltakelserSomFratarRett') WITH ORDINALITY AS deltakelser(deltakelse, rekkefølge)
    )
)
WHERE jsonb_path_exists(saksopplysninger, '$.tiltaksdeltakelserSomFratarRett[*].tittel');

UPDATE behandling
SET innvilgelsesperioder = jsonb_set(
    innvilgelsesperioder,
    '{value}',
    (
        SELECT jsonb_agg(
            jsonb_set(
                periode,
                '{tiltaksdeltakelser}',
                COALESCE(
                    (
                        SELECT jsonb_agg(
                            jsonb_set(innvilget, '{tiltaksdeltakelse}', (innvilget -> 'tiltaksdeltakelse') - 'tittel')
                            ORDER BY deltakelsesrekkefølge
                        )
                        FROM jsonb_array_elements(periode -> 'tiltaksdeltakelser') WITH ORDINALITY AS innvilgede(innvilget, deltakelsesrekkefølge)
                    ),
                    '[]'
                )
            )
            ORDER BY rekkefølge
        )
        FROM jsonb_array_elements(innvilgelsesperioder -> 'value') WITH ORDINALITY AS perioder(periode, rekkefølge)
    )
)
WHERE jsonb_path_exists(innvilgelsesperioder, '$.value[*].tiltaksdeltakelser[*].tiltaksdeltakelse.tittel');
