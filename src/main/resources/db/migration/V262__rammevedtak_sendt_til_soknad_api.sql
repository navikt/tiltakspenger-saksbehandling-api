-- Tidspunktet utfallet av søknadsbehandlingen ble sendt til tiltakspenger-soknad-api.
-- Gjelder kun vedtak i søknadsbehandlinger av digitale søknader; andre vedtak forblir null.
-- Vedtak iverksatt før kolonnen fantes er også null, så jobben sender utfallet for dem også.
ALTER TABLE rammevedtak
    ADD COLUMN IF NOT EXISTS sendt_til_soknad_api timestamptz NULL;

CREATE INDEX IF NOT EXISTS rammevedtak_ikke_sendt_til_soknad_api_idx
    ON rammevedtak (opprettet)
    WHERE sendt_til_soknad_api IS NULL;
