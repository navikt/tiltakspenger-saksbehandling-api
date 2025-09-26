ALTER TABLE søknad
    ADD COLUMN soknadstype               VARCHAR NOT NULL DEFAULT 'DIGITAL',
    ADD COLUMN manuelt_satt_soknadsperiode_fra_og_med DATE,
    ADD COLUMN manuelt_satt_soknadsperiode_til_og_med DATE;
