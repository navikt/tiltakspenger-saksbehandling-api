-- En innvilgelsesperiode har nå ett felles antall dager per meldeperiode for alle tiltaksdeltakelsene, i stedet for ett per tiltaksdeltakelse.
-- Her konverteres hver periode fra en liste med { tiltaksdeltakelse, antallDagerPerMeldeperiode } til en liste med tiltaksdeltakelsene,
-- og antall dager for perioden settes til summen av antall dager for tiltaksdeltakelsene, som var det som gjaldt for perioden.
-- Periode og reversert opphør kopieres uendret, og rekkefølgen på periodene og tiltaksdeltakelsene beholdes.
UPDATE behandling
SET innvilgelsesperioder = jsonb_build_object(
    'value',
    (
        SELECT jsonb_agg(
            (periode - 'tiltaksdeltakelser')
                || jsonb_build_object(
                    'tiltaksdeltakelser',
                    (
                        SELECT jsonb_agg(innvilget -> 'tiltaksdeltakelse' ORDER BY tiltaksrekkefølge)
                        FROM jsonb_array_elements(periode -> 'tiltaksdeltakelser') WITH ORDINALITY AS tiltak(innvilget, tiltaksrekkefølge)
                    ),
                    'antallDagerPerMeldeperiode',
                    (
                        SELECT sum((innvilget ->> 'antallDagerPerMeldeperiode')::int)
                        FROM jsonb_array_elements(periode -> 'tiltaksdeltakelser') AS tiltak(innvilget)
                    )
                )
            ORDER BY rekkefølge
        )
        FROM jsonb_array_elements(innvilgelsesperioder -> 'value') WITH ORDINALITY AS perioder(periode, rekkefølge)
    )
)
WHERE jsonb_typeof(innvilgelsesperioder -> 'value') = 'array'
  AND innvilgelsesperioder -> 'value' -> 0 -> 'tiltaksdeltakelser' -> 0 -> 'antallDagerPerMeldeperiode' IS NOT NULL;
