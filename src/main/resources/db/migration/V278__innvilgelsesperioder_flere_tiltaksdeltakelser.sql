-- En innvilgelsesperiode kan nå innvilges for flere tiltaksdeltakelser, hver med sitt eget antall dager per meldeperiode.
-- Her konverteres hver periode fra én valgt tiltaksdeltakelse til en liste med den ene tiltaksdeltakelsen.
-- Periode og reversert opphør kopieres uendret, og rekkefølgen på periodene beholdes.
UPDATE behandling
SET innvilgelsesperioder = jsonb_build_object(
    'value',
    (
        SELECT jsonb_agg(
            (periode - 'valgtTiltaksdeltakelse' - 'antallDagerPerMeldeperiode')
                || jsonb_build_object(
                    'tiltaksdeltakelser',
                    jsonb_build_array(
                        jsonb_build_object(
                            'tiltaksdeltakelse', periode -> 'valgtTiltaksdeltakelse',
                            'antallDagerPerMeldeperiode', periode -> 'antallDagerPerMeldeperiode'
                        )
                    )
                )
            ORDER BY rekkefølge
        )
        FROM jsonb_array_elements(innvilgelsesperioder -> 'value') WITH ORDINALITY AS perioder(periode, rekkefølge)
    )
)
WHERE innvilgelsesperioder -> 'value' -> 0 -> 'valgtTiltaksdeltakelse' IS NOT NULL;

-- Stønadsstatistikken får en liste med alle tiltaksdeltakelsene i hver innvilgelsesperiode, i tillegg til enkeltverdien.
-- Eksisterende perioder får en liste med verdien fra `tiltaksdeltakelse`, eller en tom liste dersom den mangler.
-- Perioder som allerede har listen beholdes uendret, og rekkefølgen på periodene beholdes.
UPDATE statistikk_stonad
SET innvilgelsesperioder = (
    SELECT jsonb_agg(
        CASE
            WHEN periode -> 'tiltaksdeltakelser' IS NOT NULL THEN periode
            WHEN jsonb_typeof(periode -> 'tiltaksdeltakelse') = 'string'
                THEN periode || jsonb_build_object('tiltaksdeltakelser', jsonb_build_array(periode -> 'tiltaksdeltakelse'))
            ELSE periode || jsonb_build_object('tiltaksdeltakelser', jsonb_build_array())
        END
        ORDER BY rekkefølge
    )
    FROM jsonb_array_elements(innvilgelsesperioder) WITH ORDINALITY AS perioder(periode, rekkefølge)
)
WHERE jsonb_typeof(innvilgelsesperioder) = 'array'
  AND EXISTS (
    SELECT 1
    FROM jsonb_array_elements(innvilgelsesperioder) AS perioder(periode)
    WHERE periode -> 'tiltaksdeltakelser' IS NULL
);
