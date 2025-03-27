ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS beregninger jsonb;

UPDATE meldekortbehandling SET beregninger = jsonb_build_array(
    jsonb_build_object(
        'kjedeId', meldeperiode_kjede_id,
        'meldekortId', id,
        'dager', meldekortdager
    )) WHERE status IN ('GODKJENT', 'KLAR_TIL_BESLUTNING') AND beregninger is null;
