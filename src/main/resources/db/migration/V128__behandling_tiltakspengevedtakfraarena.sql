UPDATE behandling
SET saksopplysninger = jsonb_set(
        saksopplysninger::jsonb,
        '{tiltakspengevedtakFraArena}',
        jsonb_build_object(
                'tiltakspengevedtakFraArena',
                COALESCE(saksopplysninger::jsonb -> 'tiltakspengevedtakFraArena', '[]'::jsonb),
                'type',
                CASE
                    WHEN saksopplysninger::jsonb -> 'tiltakspengevedtakFraArena' IS NULL
                        THEN 'BehandletFørFeature'
                    ELSE 'Treff'
                    END,
                'oppslagstidspunkt', to_jsonb(to_char(
                (opprettet)::timestamptz::timestamp,
                'YYYY-MM-DD"T"HH24:MI:SS.US')),
                'oppslagsperiode', jsonb_build_object(
                        'fraOgMed', to_jsonb(saksopplysningsperiode_fra_og_med),
                        'tilOgMed', to_jsonb(saksopplysningsperiode_til_og_med)
                                   )
        )
                       )
WHERE saksopplysninger::jsonb -> 'tiltakspengevedtakFraArena' IS NULL;