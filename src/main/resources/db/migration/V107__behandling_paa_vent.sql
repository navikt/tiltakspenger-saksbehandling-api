ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS er_satt_på_vent boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS satt_på_vent_begrunnelser jsonb DEFAULT NULL;
