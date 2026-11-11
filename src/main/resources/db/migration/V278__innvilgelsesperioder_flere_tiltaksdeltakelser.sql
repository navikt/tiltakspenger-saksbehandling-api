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
