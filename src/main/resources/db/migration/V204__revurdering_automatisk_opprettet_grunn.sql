ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS automatisk_opprettet_grunn jsonb;
