UPDATE meldekortbehandling SET meldekortdager = (
    SELECT jsonb_agg(
        jsonb_build_object(
             'dato', dag -> 'dato',
             'status', dag -> 'status'
        )
    )
    FROM jsonb_array_elements(meldekortdager) dag
);
