UPDATE utbetalingsvedtak SET status_metadata = jsonb_set(
    status_metadata,
    '{forrigeForsøk}',
    to_jsonb(
        (status_metadata->>'forrigeForsøk')::timestamptz AT TIME ZONE 'UTC'
    )
)
WHERE status_metadata IS NOT NULL;
