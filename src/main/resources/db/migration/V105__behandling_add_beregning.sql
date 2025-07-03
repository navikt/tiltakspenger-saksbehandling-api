ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS beregning jsonb;
