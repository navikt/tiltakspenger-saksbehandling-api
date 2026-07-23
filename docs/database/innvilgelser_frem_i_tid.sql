WITH iverksatte AS (
    SELECT
        b.id,
        b.sak_id,
        b.iverksatt_tidspunkt,
        daterange(b.virkningsperiode_fra_og_med, b.virkningsperiode_til_og_med, '[]') AS vedtaksperiode,
        coalesce(
                (SELECT range_agg(daterange((p -> 'periode' ->> 'fraOgMed')::date,
                                            (p -> 'periode' ->> 'tilOgMed')::date, '[]'))
                 FROM jsonb_array_elements(b.innvilgelsesperioder -> 'value') AS p),
                '{}'::datemultirange
        ) AS innvilget
    FROM behandling b
    WHERE b.iverksatt_tidspunkt IS NOT NULL
      AND b.virkningsperiode_fra_og_med IS NOT NULL
      AND b.resultat IS DISTINCT FROM 'AVSLAG'
    ),
    bidrag AS (
SELECT
    sak_id,
    innvilget - coalesce(
    range_agg(vedtaksperiode) OVER (
    PARTITION BY sak_id
    ORDER BY iverksatt_tidspunkt DESC, id DESC
    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
    ),
    '{}'::datemultirange
    ) AS gjeldende_innvilget
FROM iverksatte
    ),
    per_sak AS (
SELECT
    sak_id,
    min(lower(r)) - (now() AT TIME ZONE 'Europe/Oslo')::date AS antall_dager
FROM bidrag, unnest(gjeldende_innvilget) AS r
GROUP BY sak_id
    )
SELECT
    s.saksnummer,
    p.antall_dager
FROM per_sak p
         JOIN sak s ON s.id = p.sak_id
WHERE p.antall_dager >= 1
ORDER BY p.antall_dager, s.saksnummer;