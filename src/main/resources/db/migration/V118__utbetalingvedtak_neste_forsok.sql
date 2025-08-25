-- Patch data til å ha nesteForsøk i status_metadata, settes til nå.
UPDATE utbetalingsvedtak
SET status_metadata = jsonb_set(
        status_metadata,
        '{nesteForsøk}',
        to_jsonb(now()::timestamp),
        true
                      )
WHERE NOT status_metadata ? 'nesteForsøk';

UPDATE utbetalingsvedtak
SET status_metadata = jsonb_build_object(
        'forrigeForsøk', NULL,
        'antallForsøk', 0,
        'nesteForsøk', now()::timestamp
                      )
WHERE status_metadata IS NULL;
