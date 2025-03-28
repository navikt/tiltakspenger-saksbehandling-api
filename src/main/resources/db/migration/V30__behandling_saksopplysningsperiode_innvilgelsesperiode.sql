ALTER TABLE behandling ADD COLUMN IF NOT EXISTS saksopplysningsperiode_fra_og_med date;
ALTER TABLE behandling ADD COLUMN IF NOT EXISTS saksopplysningsperiode_til_og_med date;
ALTER TABLE behandling ADD COLUMN IF NOT EXISTS innvilgelsesperiode_fra_og_med date;
ALTER TABLE behandling ADD COLUMN IF NOT EXISTS innvilgelsesperiode_til_og_med date;
ALTER TABLE behandling ALTER COLUMN fra_og_med DROP NOT NULL;
ALTER TABLE behandling ALTER COLUMN til_og_med DROP NOT NULL;