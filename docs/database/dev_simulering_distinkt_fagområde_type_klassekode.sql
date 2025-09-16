SELECT distinct
    postering->>'fagområde' AS fagområde,
    postering->>'type' AS type,
    postering->>'klassekode' AS klassekode

FROM behandling,
     jsonb_array_elements(simulering_metadata::jsonb->'detaljer'->'perioder') AS periode,
     jsonb_array_elements(periode->'posteringer') AS postering
WHERE simulering_metadata IS NOT NULL
  AND simulering_metadata != ''

UNION

SELECT DISTINCT
    postering->>'fagområde' AS fagområde,
    postering->>'type' AS type,
    postering->>'klassekode' AS klassekode
FROM meldekortbehandling,
     jsonb_array_elements(simulering_metadata::jsonb->'detaljer'->'perioder') AS periode,
     jsonb_array_elements(periode->'posteringer') AS postering
WHERE simulering_metadata IS NOT NULL
  AND simulering_metadata != '';