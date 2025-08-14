UPDATE behandling
SET saksopplysninger = jsonb_set(
        saksopplysninger::jsonb,
        '{ytelser,oppslagstidspunkt}',
        to_jsonb(
                to_char(
                        (saksopplysninger::jsonb->'ytelser'->>'oppslagstidspunkt')::timestamptz::timestamp,
                        'YYYY-MM-DD"T"HH24:MI:SS.US'
                )
        )
                       )
WHERE
    saksopplysninger::jsonb->'ytelser'->'oppslagstidspunkt' IS NOT NULL
  AND (saksopplysninger::jsonb->'ytelser'->>'oppslagstidspunkt') ~ '\+\d{2}:\d{2}$';