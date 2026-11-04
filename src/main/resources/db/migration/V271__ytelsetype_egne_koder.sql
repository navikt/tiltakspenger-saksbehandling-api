-- Oppslag avgrenset med regelen fra før 2026-11-03 16:55 slettes, med margin; jobben henter dem på nytt.
DELETE FROM utbetalingsoversikt
WHERE hentet < timestamptz '2026-11-03 17:30:00 Europe/Oslo';

-- Barnepensjon hindrer ikke tiltakspenger (tiltakspengeforskriften § 7 første ledd), men ble hentet fram til 2026-10-20; ingen vedtak bygde på den.
-- Et Treff som bare hadde barnepensjon, blir IngenTreff, slik et nytt oppslag i dag ville gitt.
UPDATE behandling b
SET saksopplysninger = jsonb_set(
    jsonb_set(b.saksopplysninger::jsonb, '{ytelser,ytelser}', t.ytelser),
    '{ytelser,type}',
    CASE WHEN t.ytelser = '[]'::jsonb THEN '"IngenTreff"'::jsonb ELSE b.saksopplysninger::jsonb -> 'ytelser' -> 'type' END
)
FROM (
    SELECT b2.id,
           coalesce(jsonb_agg(y ORDER BY rekkefølge) FILTER (WHERE y ->> 'ytelsetype' <> 'Barnepensjon'), '[]'::jsonb) AS ytelser
    FROM behandling b2,
         jsonb_array_elements(b2.saksopplysninger::jsonb -> 'ytelser' -> 'ytelser') WITH ORDINALITY AS e(y, rekkefølge)
    WHERE b2.saksopplysninger::jsonb -> 'ytelser' -> 'ytelser' @> '[{"ytelsetype": "Barnepensjon"}]'
    GROUP BY b2.id
) t
WHERE b.id = t.id;

-- Ytelsetypen lagres med våre egne koder; på behandlingen beholdes teksten fra sokos-utbetaldata som kildetekst.
-- «Ukjent» var vår egen tekst for en ytelse uten type, så den får verken kode eller kildetekst.
CREATE TEMPORARY TABLE ytelsetype_kode (tekst text PRIMARY KEY, kode text);
INSERT INTO ytelsetype_kode (tekst, kode) VALUES
    ('Arbeidsavklaringspenger', 'AAP'),
    ('Avtalefestet pensjon', 'AFP'),
    ('Alderspensjon', 'ALDERSPENSJON'),
    ('Dagpenger', 'DAGPENGER'),
    ('Enslig forsørger', 'ENSLIG_FORSORGER'),
    ('Etterlattepensjon', 'ETTERLATTEPENSJON'),
    ('Foreldrepenger', 'FORELDREPENGER'),
    ('Foreldrepenger refusjon arbeidsgiver', 'FORELDREPENREFARBGIV'),
    ('Kompensasjon inntektstap', 'KOMPENSASJON'),
    ('Krigspensjon', 'KRIGSPENSJON'),
    ('Lønnskompensasjon', 'LONNSKOMPENSASJON'),
    ('Omsorgspenger', 'OMSORGSPENGER'),
    ('Omstillingsstønad', 'OMSTILLINGSSTONAD'),
    ('Opplæringspenger', 'OPPLAERINGSPENGER'),
    ('Pensjon reindriftssamer', 'PEN_REINDRIFTSSAMER'),
    ('Pleiepenger', 'PLEIEPENGER'),
    ('Pleiepenger refusjon arbeidgiver', 'PLEIEPENGERREFARBGIV'),
    ('Refusjon', 'REFUSJON'),
    ('Refusjon PST', 'REFUSJON_PST'),
    ('Refusjon SPK', 'REFUSJON_SPK'),
    ('Refusjonskrav', 'REFUSJONSKRAV'),
    ('Statens pensjonskasse', 'SPK'),
    ('Statens pensjonkasse', 'SPK'),
    ('Supplerende_stønad', 'SUPPLERENDE_STONAD'),
    ('Svangerskapspenger', 'SVANGERSKAPSPENGER'),
    ('Sykepenger', 'SYKEPENGER'),
    ('Sykepenger refusjon arbeidsgiver', 'SYKEPENGERREFARBGIV'),
    ('Tjenestepensjon', 'TJENESTEPENSJON'),
    ('Tiltakspenger', 'TILTAKSPENGER'),
    ('Uførepensjon', 'UFOREPENSJON'),
    ('Uføretrygd', 'UFORETRYGD'),
    ('Ungdomsprogram ytelse', 'UNGDOMSPROGRAMYTELSE'),
    ('Ukjent', NULL);

-- Stopper migreringen i stedet for å miste en tekst vi ikke kjenner.
DO $$
DECLARE
    ukjente text;
BEGIN
    SELECT string_agg(DISTINCT coalesce(tekst, '<null>'), ', ') INTO ukjente
    FROM (
        SELECT y ->> 'ytelsetype' AS tekst, true AS kan_være_ukjent
        FROM behandling b,
             jsonb_array_elements(b.saksopplysninger::jsonb -> 'ytelser' -> 'ytelser') y
        WHERE jsonb_typeof(b.saksopplysninger::jsonb -> 'ytelser' -> 'ytelser') = 'array'
        UNION ALL
        SELECT y ->> 'ytelsestype', false
        FROM utbetalingsoversikt u,
             jsonb_array_elements(u.utbetalinger -> 'utbetalinger') ut,
             jsonb_array_elements(ut -> 'ytelser') y
        WHERE u.utbetalinger IS NOT NULL
    ) t
    WHERE NOT EXISTS (
        SELECT 1 FROM ytelsetype_kode k
        WHERE k.tekst = t.tekst AND (k.kode IS NOT NULL OR t.kan_være_ukjent)
    );
    IF ukjente IS NOT NULL THEN
        RAISE EXCEPTION 'Ytelsestyper uten kode: %', ukjente;
    END IF;
END
$$;

UPDATE behandling b
SET saksopplysninger = jsonb_set(
    b.saksopplysninger::jsonb,
    '{ytelser,ytelser}',
    (
        SELECT jsonb_agg(
            y || jsonb_build_object(
                'ytelsetype', k.kode,
                'kildetekst', CASE WHEN k.kode IS NULL THEN NULL ELSE k.tekst END
            )
            ORDER BY rekkefølge
        )
        FROM jsonb_array_elements(b.saksopplysninger::jsonb -> 'ytelser' -> 'ytelser') WITH ORDINALITY AS e(y, rekkefølge)
        JOIN ytelsetype_kode k ON k.tekst = y ->> 'ytelsetype'
    )
)
WHERE jsonb_typeof(b.saksopplysninger::jsonb -> 'ytelser' -> 'ytelser') = 'array'
  AND jsonb_array_length(b.saksopplysninger::jsonb -> 'ytelser' -> 'ytelser') > 0;

UPDATE utbetalingsoversikt u
SET utbetalinger = jsonb_build_object(
    'utbetalinger',
    (
        SELECT coalesce(jsonb_agg(
            ut || jsonb_build_object(
                'ytelser',
                (
                    SELECT coalesce(jsonb_agg(
                        (y - 'ytelsestype') || jsonb_build_object('ytelsetype', k.kode)
                        ORDER BY ytelserekkefølge
                    ), '[]'::jsonb)
                    FROM jsonb_array_elements(ut -> 'ytelser') WITH ORDINALITY AS ye(y, ytelserekkefølge)
                    JOIN ytelsetype_kode k ON k.tekst = y ->> 'ytelsestype'
                )
            )
            ORDER BY utbetalingsrekkefølge
        ), '[]'::jsonb)
        FROM jsonb_array_elements(u.utbetalinger -> 'utbetalinger') WITH ORDINALITY AS ue(ut, utbetalingsrekkefølge)
    )
)
WHERE u.utbetalinger IS NOT NULL;

DROP TABLE ytelsetype_kode;
