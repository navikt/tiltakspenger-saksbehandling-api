ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS satt_paa_vent jsonb DEFAULT NULL;
