-- Migrer utbetalingskontroll fra double-encoded JSON-strenger til proper JSONB.
-- Før: {"beregning": "<escaped json string>", "simulering": "<escaped json string>"}
-- Etter: {"beregning": { ... }, "simulering": { ... }}
UPDATE behandling
SET utbetalingskontroll = jsonb_build_object(
    'beregning', (utbetalingskontroll->>'beregning')::jsonb,
    'simulering', (utbetalingskontroll->>'simulering')::jsonb
)
WHERE utbetalingskontroll IS NOT NULL;

-- Migrer beregning i utbetalingskontroll fra rå array til BeregningDbJson-objekt med beregningstidspunkt.
-- Før: {"beregning": [...], "simulering": {...}}
-- Etter: {"beregning": {"beregninger": [...], "beregningstidspunkt": "..."}, "simulering": {...}}
UPDATE behandling
SET utbetalingskontroll = jsonb_set(
    utbetalingskontroll,
    '{beregning}',
    jsonb_build_object(
        'beregninger', utbetalingskontroll->'beregning',
        'beregningstidspunkt', utbetalingskontroll->'simulering'->>'simuleringstidspunkt'
    )
)
WHERE utbetalingskontroll IS NOT NULL
  AND jsonb_typeof(utbetalingskontroll->'beregning') = 'array';
