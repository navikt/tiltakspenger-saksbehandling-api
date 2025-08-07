ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS ventestatus jsonb DEFAULT NULL;
