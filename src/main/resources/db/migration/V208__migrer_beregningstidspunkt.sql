UPDATE behandling
SET beregning = jsonb_build_object(
    'beregninger', beregning,
    'beregningstidspunkt', simulering->>'simuleringstidspunkt'
)
WHERE
    beregning IS NOT NULL
    AND jsonb_typeof(beregning) = 'array'
    AND simulering IS NOT NULL
    AND simulering->>'simuleringstidspunkt' IS NOT NULL;

UPDATE behandling
SET beregning = jsonb_set(
    beregning,
    '{beregningstidspunkt}',
    to_jsonb(simulering->>'simuleringstidspunkt')
)
WHERE
    beregning IS NOT NULL
    AND jsonb_typeof(beregning) = 'object'
    AND beregning ? 'beregninger'
    AND beregning->>'beregningstidspunkt' IS NULL
    AND simulering IS NOT NULL
    AND simulering->>'simuleringstidspunkt' IS NOT NULL;