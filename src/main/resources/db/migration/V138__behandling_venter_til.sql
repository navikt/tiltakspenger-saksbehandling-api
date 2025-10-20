ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS venter_til timestamptz NULL;
