ALTER TABLE søknad
    ADD COLUMN IF NOT EXISTS soknadstype               VARCHAR NOT NULL DEFAULT 'DIGITAL',
    ADD COLUMN IF NOT EXISTS manuelt_satt_soknadsperiode_fra_og_med DATE,
    ADD COLUMN IF NOT EXISTS manuelt_satt_soknadsperiode_til_og_med DATE;
