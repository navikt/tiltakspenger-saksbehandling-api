UPDATE behandling
SET saksopplysninger = jsonb_set(
    saksopplysninger::jsonb,
    '{ytelser}',
    jsonb_build_object(
        'ytelser', COALESCE(saksopplysninger::jsonb->'ytelser', '[]'::jsonb),
        'type',
        CASE
            WHEN saksopplysninger::jsonb->'ytelser' IS NULL
                OR saksopplysninger::jsonb->'ytelser' = '[]'::jsonb
            THEN 'BehandletFørFeature'
            ELSE 'Treff'
        END,
        'oppslagstidspunkt', to_jsonb(opprettet),
        'oppslagsperiode', jsonb_build_object(
            'fraOgMed', to_jsonb(saksopplysningsperiode_fra_og_med),
            'tilOgMed', to_jsonb(saksopplysningsperiode_til_og_med)
        )
    )
)
WHERE
    jsonb_typeof(saksopplysninger::jsonb->'ytelser') = 'array'
    OR saksopplysninger::jsonb->'ytelser' IS NULL;