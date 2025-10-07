ALTER TABLE sak
    ADD COLUMN IF NOT EXISTS skal_sende_meldeperioder_til_datadeling BOOLEAN NOT NULL default false;