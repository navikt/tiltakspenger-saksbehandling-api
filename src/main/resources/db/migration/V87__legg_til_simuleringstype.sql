UPDATE meldekortbehandling
SET simulering = simulering || '{"type": "ENDRING"}'::jsonb
WHERE simulering IS NOT NULL;