ALTER TABLE behandling
    DROP COLUMN IF EXISTS saksopplysningsperiode_fra_og_med;

ALTER TABLE behandling
    DROP COLUMN IF EXISTS saksopplysningsperiode_til_og_med;

UPDATE behandling
SET saksopplysninger = jsonb_set(
    saksopplysninger,
    '{oppslagstidspunkt}',
    to_jsonb(
        COALESCE(
            (saksopplysninger -> 'ytelser' ->> 'oppslagstidspunkt'),
            (saksopplysninger -> 'tiltakspengevedtakFraArena' ->> 'oppslagstidspunkt'),
            opprettet::text
        )
    )
);
