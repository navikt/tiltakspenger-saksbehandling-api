UPDATE behandling
SET saksopplysninger = jsonb_set(
    saksopplysninger,
    '{oppslagstidspunkt}',
    to_jsonb(
        COALESCE(
            (saksopplysninger -> 'ytelser' ->> 'oppslagstidspunkt'),
            (saksopplysninger -> 'tiltakspengevedtakFraArena' ->> 'oppslagstidspunkt'),
            to_char(opprettet, 'YYYY-MM-DD"T"HH24:MI:SS.US') -- Kjører update fra V145 på nytt med riktig format på denne
        )
    )
);
