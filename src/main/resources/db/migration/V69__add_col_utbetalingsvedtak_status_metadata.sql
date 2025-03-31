ALTER TABLE utbetalingsvedtak ADD COLUMN status_metadata JSONB;

UPDATE utbetalingsvedtak
SET status_metadata = jsonb_build_object(
  'forrigeForsøk', now(),
  'antallForsøk', 1
)
WHERE status IS NOT NULL;
