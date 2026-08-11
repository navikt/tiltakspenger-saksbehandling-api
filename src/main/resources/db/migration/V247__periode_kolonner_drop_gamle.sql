-- Dropper fra/til-kolonnene som ble erstattet av periode-kolonner i V244.
ALTER TABLE behandling
    DROP COLUMN IF EXISTS virkningsperiode_fra_og_med,
    DROP COLUMN IF EXISTS virkningsperiode_til_og_med;

-- periode-kolonnen er ikke i bruk heller
ALTER TABLE meldekortbehandling
    DROP COLUMN IF EXISTS fra_og_med,
    DROP COLUMN IF EXISTS til_og_med,
    DROP COLUMN IF EXISTS periode;

ALTER TABLE meldeperiode
    DROP COLUMN IF EXISTS fra_og_med,
    DROP COLUMN IF EXISTS til_og_med;
-- idx_meldeperiode_periode forsvant med de gamle kolonnene og gjenskapes på feltene i den nye.
CREATE INDEX IF NOT EXISTS idx_meldeperiode_periode ON meldeperiode (((periode).fra_og_med), ((periode).til_og_med));

ALTER TABLE rammevedtak
    DROP COLUMN IF EXISTS fra_og_med,
    DROP COLUMN IF EXISTS til_og_med;

ALTER TABLE søknad
    DROP COLUMN IF EXISTS kvp_fom,
    DROP COLUMN IF EXISTS kvp_tom,
    DROP COLUMN IF EXISTS intro_fom,
    DROP COLUMN IF EXISTS intro_tom,
    DROP COLUMN IF EXISTS institusjon_fom,
    DROP COLUMN IF EXISTS institusjon_tom,
    DROP COLUMN IF EXISTS sykepenger_fom,
    DROP COLUMN IF EXISTS sykepenger_tom,
    DROP COLUMN IF EXISTS supplerende_alder_fom,
    DROP COLUMN IF EXISTS supplerende_alder_tom,
    DROP COLUMN IF EXISTS supplerende_flyktning_fom,
    DROP COLUMN IF EXISTS supplerende_flyktning_tom,
    DROP COLUMN IF EXISTS jobbsjansen_fom,
    DROP COLUMN IF EXISTS jobbsjansen_tom,
    DROP COLUMN IF EXISTS gjenlevendepensjon_fom,
    DROP COLUMN IF EXISTS gjenlevendepensjon_tom,
    DROP COLUMN IF EXISTS trygd_og_pensjon_fom,
    DROP COLUMN IF EXISTS trygd_og_pensjon_tom,
    DROP COLUMN IF EXISTS manuelt_satt_soknadsperiode_fra_og_med,
    DROP COLUMN IF EXISTS manuelt_satt_soknadsperiode_til_og_med;

ALTER TABLE søknadstiltak
    DROP COLUMN IF EXISTS deltakelse_fra_og_med,
    DROP COLUMN IF EXISTS deltakelse_til_og_med;

ALTER TABLE tiltaksdeltaker_kafka
    DROP COLUMN IF EXISTS deltakelse_fra_og_med,
    DROP COLUMN IF EXISTS deltakelse_til_og_med;
