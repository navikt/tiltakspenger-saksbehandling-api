ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS beregning jsonb;

ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS navkontor VARCHAR;

ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS navkontor_navn VARCHAR;
