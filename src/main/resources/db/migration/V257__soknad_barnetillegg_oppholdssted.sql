-- Hvor barnet oppholder seg i tiltaksperioden: NORGE, ANNET_EØS_LAND eller IKKE_EØS_LAND.
-- Null for søknader fra før søknaden spurte om dette; de har bare ja/nei i opphold_i_eos_type.
ALTER TABLE søknad_barnetillegg
    ADD COLUMN oppholdssted VARCHAR;
