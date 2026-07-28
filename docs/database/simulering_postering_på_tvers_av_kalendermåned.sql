-- Finnes det posteringer i simulering_metadata der fom og tom ligger i ulike kalendermåneder?
-- Tomt resultat betyr nei, altså at oppdragssystemet alltid splitter posteringer på månedsskifte.
-- Fagområde-filteret speiler domenet, som kun konsumerer TILTAKSPENGER-posteringer; fjern det for å se alle fagområder.
WITH posteringer AS (
    SELECT 'behandling' AS kilde,
           b.id,
           b.sak_id,
           (postering ->> 'fom')::date AS fom,
           (postering ->> 'tom')::date AS tom,
           postering ->> 'type' AS type,
           postering ->> 'klassekode' AS klassekode,
           (postering ->> 'beløp')::int AS beløp
    FROM behandling b,
         jsonb_array_elements(b.simulering_metadata::jsonb -> 'detaljer' -> 'perioder') AS periode,
         jsonb_array_elements(periode -> 'posteringer') AS postering
    WHERE b.simulering_metadata IS NOT NULL
      AND b.simulering_metadata != ''
      AND postering ->> 'fagområde' = 'TILTAKSPENGER'

    UNION ALL

    SELECT 'meldekortbehandling' AS kilde,
           m.id,
           m.sak_id,
           (postering ->> 'fom')::date,
           (postering ->> 'tom')::date,
           postering ->> 'type',
           postering ->> 'klassekode',
           (postering ->> 'beløp')::int
    FROM meldekortbehandling m,
         jsonb_array_elements(m.simulering_metadata::jsonb -> 'detaljer' -> 'perioder') AS periode,
         jsonb_array_elements(periode -> 'posteringer') AS postering
    WHERE m.simulering_metadata IS NOT NULL
      AND m.simulering_metadata != ''
      AND postering ->> 'fagområde' = 'TILTAKSPENGER'
)
SELECT kilde,
       id,
       sak_id,
       fom,
       tom,
       type,
       klassekode,
       beløp
FROM posteringer
WHERE date_trunc('month', fom) <> date_trunc('month', tom)
ORDER BY sak_id, fom;
