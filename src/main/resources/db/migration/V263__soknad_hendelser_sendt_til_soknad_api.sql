-- Antall av søknadens avbrudd og gjenåpninger (søknad.avbrutt) som er sendt til tiltakspenger-soknad-api.
-- Hendelsene legges kun til, så søknaden ligger i køen så lenge den har flere hendelser enn det som er sendt.
-- Søknader som ble avbrutt før kolonnen fantes starter på 0, så jobben sender utfallet for dem også.
ALTER TABLE søknad
    ADD COLUMN IF NOT EXISTS hendelser_sendt_til_soknad_api integer NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS soknad_hendelser_ikke_sendt_til_soknad_api_idx
    ON søknad (opprettet)
    WHERE jsonb_array_length(avbrutt) > hendelser_sendt_til_soknad_api;
