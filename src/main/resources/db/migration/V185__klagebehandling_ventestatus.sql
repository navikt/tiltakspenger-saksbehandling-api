ALTER TABLE klagebehandling
    ADD COLUMN IF NOT EXISTS ventestatus jsonb DEFAULT NULL;