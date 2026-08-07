-- Flytt denne til /migration asap

-- Dropper fra/til-kolonnene som ble erstattet av periode-kolonner i V244.
ALTER TABLE behandling
    DROP COLUMN virkningsperiode_fra_og_med,
    DROP COLUMN virkningsperiode_til_og_med;

ALTER TABLE meldekortbehandling
    DROP COLUMN fra_og_med,
    DROP COLUMN til_og_med;

ALTER TABLE meldeperiode
    DROP COLUMN fra_og_med,
    DROP COLUMN til_og_med;
-- idx_meldeperiode_periode forsvant med de gamle kolonnene og gjenskapes på feltene i den nye.
CREATE INDEX idx_meldeperiode_periode ON meldeperiode (((periode).fra_og_med), ((periode).til_og_med));

ALTER TABLE rammevedtak
    DROP COLUMN fra_og_med,
    DROP COLUMN til_og_med;

ALTER TABLE søknad
    DROP COLUMN kvp_fom,
    DROP COLUMN kvp_tom,
    DROP COLUMN intro_fom,
    DROP COLUMN intro_tom,
    DROP COLUMN institusjon_fom,
    DROP COLUMN institusjon_tom,
    DROP COLUMN sykepenger_fom,
    DROP COLUMN sykepenger_tom,
    DROP COLUMN supplerende_alder_fom,
    DROP COLUMN supplerende_alder_tom,
    DROP COLUMN supplerende_flyktning_fom,
    DROP COLUMN supplerende_flyktning_tom,
    DROP COLUMN jobbsjansen_fom,
    DROP COLUMN jobbsjansen_tom,
    DROP COLUMN gjenlevendepensjon_fom,
    DROP COLUMN gjenlevendepensjon_tom,
    DROP COLUMN trygd_og_pensjon_fom,
    DROP COLUMN trygd_og_pensjon_tom,
    DROP COLUMN manuelt_satt_soknadsperiode_fra_og_med,
    DROP COLUMN manuelt_satt_soknadsperiode_til_og_med;

ALTER TABLE søknadstiltak
    DROP COLUMN deltakelse_fra_og_med,
    DROP COLUMN deltakelse_til_og_med;

ALTER TABLE tiltaksdeltaker_kafka
    DROP COLUMN deltakelse_fra_og_med,
    DROP COLUMN deltakelse_til_og_med;
