ALTER TABLE søknad
    ADD COLUMN soknadstype               VARCHAR NOT NULL DEFAULT 'DIGITAL',
    ADD COLUMN soknadsperiode_fra_og_med DATE,
    ADD COLUMN soknadsperiode_til_og_med DATE;
