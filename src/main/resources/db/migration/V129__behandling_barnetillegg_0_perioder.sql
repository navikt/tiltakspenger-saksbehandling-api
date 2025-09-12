-- Setter barnetillegg til en 0-periode istedenfor null, dersom barnetillegg ikke var innvilget
UPDATE behandling b SET barnetillegg = jsonb_build_object(
    'value',
    jsonb_build_array(
        jsonb_build_object(
            'verdi',
            0,
            'periode',
            jsonb_build_object(
                'fraOgMed', to_jsonb(b.virkningsperiode_fra_og_med),
                'tilOgMed', to_jsonb(b.virkningsperiode_til_og_med)
            )
        )
    ),
    'begrunnelse',
    to_jsonb(null::text)
)
WHERE barnetillegg is null
  AND status IN ('KLAR_TIL_BESLUTNING', 'UNDER_BESLUTNING', 'VEDTATT')