UPDATE klagebehandling
SET avbrutt = jsonb_set(avbrutt, '{status}', '"ANNET"'::jsonb, true)
WHERE status = 'AVBRUTT'
  AND avbrutt IS NOT NULL
  AND (avbrutt ->> 'status') IS NULL;

