-- Finnes det posteringer i simulering_metadata som overlapper to eller flere meldeperioder på saken?
-- Tomt resultat betyr nei.
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
SELECT p.kilde,
       p.id,
       p.sak_id,
       p.fom,
       p.tom,
       p.type,
       p.klassekode,
       p.beløp,
       count(DISTINCT mp.kjede_id) AS antall_meldeperioder,
       array_agg(DISTINCT mp.kjede_id) AS meldeperioder
FROM posteringer p
         JOIN meldeperiode mp
              ON mp.sak_id = p.sak_id
                  AND mp.fra_og_med <= p.tom
                  AND mp.til_og_med >= p.fom
GROUP BY p.kilde, p.id, p.sak_id, p.fom, p.tom, p.type, p.klassekode, p.beløp
HAVING count(DISTINCT mp.kjede_id) >= 2
ORDER BY p.sak_id, p.fom;
