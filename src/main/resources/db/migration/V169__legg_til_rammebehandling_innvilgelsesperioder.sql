ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS innvilgelsesperioder JSONB NULL;
