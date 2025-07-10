ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS er_satt_paa_vent boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS satt_paa_vent_begrunnelser jsonb DEFAULT NULL;
