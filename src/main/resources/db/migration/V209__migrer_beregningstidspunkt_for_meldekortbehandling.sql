-- Update old format (array) rows
UPDATE meldekortbehandling
SET beregninger = jsonb_build_object(
    'beregninger', beregninger,
    'beregningstidspunkt', simulering->>'simuleringstidspunkt'
)
WHERE
    beregninger IS NOT NULL
    AND jsonb_typeof(beregninger) = 'array'
    AND simulering IS NOT NULL
    AND simulering->>'simuleringstidspunkt' IS NOT NULL;

-- Update new-format objects where beregningstidspunkt is still null
UPDATE meldekortbehandling
SET beregninger = jsonb_set(
    beregninger,
    '{beregningstidspunkt}',
    to_jsonb(simulering->>'simuleringstidspunkt')
)
WHERE
    beregninger IS NOT NULL
    AND jsonb_typeof(beregninger) = 'object'
    AND beregninger ? 'beregninger'
    AND beregninger->>'beregningstidspunkt' IS NULL
    AND simulering IS NOT NULL
    AND simulering->>'simuleringstidspunkt' IS NOT NULL;

-- Old format (array), simulering null, beslutter = 'tp-sak'
UPDATE meldekortbehandling
SET beregninger = jsonb_build_object(
    'beregninger', beregninger,
    'beregningstidspunkt', to_char(opprettet AT TIME ZONE 'Europe/Oslo', 'YYYY-MM-DD"T"HH24:MI:SS.MS')
)
WHERE
    beregninger IS NOT NULL
    AND jsonb_typeof(beregninger) = 'array'
    AND (simulering IS NULL OR simulering->>'simuleringstidspunkt' IS NULL)
    AND beslutter = 'tp-sak';

-- Old format (array), simulering null, beslutter != 'tp-sak'
UPDATE meldekortbehandling
SET beregninger = jsonb_build_object(
    'beregninger', beregninger,
    'beregningstidspunkt', to_char((sendt_til_beslutning - interval '1 second') AT TIME ZONE 'Europe/Oslo', 'YYYY-MM-DD"T"HH24:MI:SS.MS')
)
WHERE
    beregninger IS NOT NULL
    AND jsonb_typeof(beregninger) = 'array'
    AND (simulering IS NULL OR simulering->>'simuleringstidspunkt' IS NULL)
    AND beslutter != 'tp-sak'
    AND sendt_til_beslutning IS NOT NULL;