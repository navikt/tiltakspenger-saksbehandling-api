ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS simulering_metadata VARCHAR;

ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS simulering JSONB;
