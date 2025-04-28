UPDATE meldekort_bruker SET dager = (
    SELECT jsonb_agg(
        jsonb_set(
            elem,
            '{status}',
            to_jsonb(
            CASE elem->>'status'
                WHEN 'DELTATT' THEN 'DELTATT_UTEN_LØNN_I_TILTAKET'
                WHEN 'IKKE_DELTATT' THEN 'FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV'
                WHEN 'FRAVÆR_ANNET' THEN 'FRAVÆR_VELFERD_GODKJENT_AV_NAV'
                ELSE elem->>'status'
                END
            )
        )
) FROM jsonb_array_elements(dager) AS arr(elem));
