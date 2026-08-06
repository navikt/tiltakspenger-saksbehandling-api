-- Domene for åpne perioder: den ene enden kan mangle (f.eks. en pågående tiltaksdeltakelse uten kjent sluttdato).
-- Når begge ender er satt, må fra_og_med være før eller lik til_og_med.
-- Merk postgres' null-semantikk for sammensatte typer: en verdi der begge felter er null regnes som NULL,
-- og en verdi der bare det ene feltet er null er verken IS NULL eller IS NOT NULL.
CREATE DOMAIN periode_open AS periode_datoer
    CHECK (
        VALUE IS NULL OR
        (VALUE).fra_og_med IS NULL OR
        (VALUE).til_og_med IS NULL OR
        (VALUE).fra_og_med <= (VALUE).til_og_med
    );

-- behandling: kolonnene het virkningsperiode_*, men innholdet er vedtaksperioden (jf. TODO i RammebehandlingDb).
ALTER TABLE behandling
    ADD COLUMN vedtaksperiode periode;
UPDATE behandling
SET vedtaksperiode = ROW (virkningsperiode_fra_og_med, virkningsperiode_til_og_med)::periode
WHERE virkningsperiode_fra_og_med IS NOT NULL;
ALTER TABLE behandling
    DROP COLUMN virkningsperiode_fra_og_med,
    DROP COLUMN virkningsperiode_til_og_med;

ALTER TABLE meldekortbehandling
    ADD COLUMN periode periode;
UPDATE meldekortbehandling
SET periode = ROW (fra_og_med, til_og_med)::periode;
ALTER TABLE meldekortbehandling
    ALTER COLUMN periode SET NOT NULL,
    DROP COLUMN fra_og_med,
    DROP COLUMN til_og_med;

ALTER TABLE meldeperiode
    ADD COLUMN periode periode;
UPDATE meldeperiode
SET periode = ROW (fra_og_med, til_og_med)::periode;
ALTER TABLE meldeperiode
    ALTER COLUMN periode SET NOT NULL,
    DROP COLUMN fra_og_med,
    DROP COLUMN til_og_med;
-- idx_meldeperiode_periode forsvant med de gamle kolonnene og gjenskapes på feltene i den nye.
CREATE INDEX idx_meldeperiode_periode ON meldeperiode (((periode).fra_og_med), ((periode).til_og_med));

ALTER TABLE rammevedtak
    ADD COLUMN periode periode;
UPDATE rammevedtak
SET periode = ROW (fra_og_med, til_og_med)::periode;
ALTER TABLE rammevedtak
    ALTER COLUMN periode SET NOT NULL,
    DROP COLUMN fra_og_med,
    DROP COLUMN til_og_med;

ALTER TABLE statistikk_meldekort
    ADD COLUMN periode periode;
UPDATE statistikk_meldekort
SET periode = ROW (fra_og_med, til_og_med)::periode;
ALTER TABLE statistikk_meldekort
    ALTER COLUMN periode SET NOT NULL,
    DROP COLUMN fra_og_med,
    DROP COLUMN til_og_med;

ALTER TABLE statistikk_sak
    ADD COLUMN funksjonellperiode periode;
UPDATE statistikk_sak
SET funksjonellperiode = ROW (funksjonellperiode_fra_og_med, funksjonellperiode_til_og_med)::periode
WHERE funksjonellperiode_fra_og_med IS NOT NULL;
ALTER TABLE statistikk_sak
    DROP COLUMN funksjonellperiode_fra_og_med,
    DROP COLUMN funksjonellperiode_til_og_med;

ALTER TABLE statistikk_stonad
    ADD COLUMN vedtaksperiode periode;
UPDATE statistikk_stonad
SET vedtaksperiode = ROW (vedtaksperiode_fra_og_med, vedtaksperiode_til_og_med)::periode
WHERE vedtaksperiode_fra_og_med IS NOT NULL;
ALTER TABLE statistikk_stonad
    DROP COLUMN vedtaksperiode_fra_og_med,
    DROP COLUMN vedtaksperiode_til_og_med;

-- Søknadens periodespørsmål kan være åpne i den ene enden, derav periode_open.
ALTER TABLE søknad
    ADD COLUMN kvp_periode periode_open,
    ADD COLUMN intro_periode periode_open,
    ADD COLUMN institusjon_periode periode_open,
    ADD COLUMN sykepenger_periode periode_open,
    ADD COLUMN supplerende_alder_periode periode_open,
    ADD COLUMN supplerende_flyktning_periode periode_open,
    ADD COLUMN jobbsjansen_periode periode_open,
    ADD COLUMN gjenlevendepensjon_periode periode_open,
    ADD COLUMN trygd_og_pensjon_periode periode_open,
    ADD COLUMN manuelt_satt_soknadsperiode periode;
-- ROW(NULL, NULL) lagres som en tom composit-verdi i stedet for NULL, så kolonnene settes bare når minst én ende finnes.
UPDATE søknad
SET kvp_periode                   = CASE WHEN kvp_fom IS NULL AND kvp_tom IS NULL THEN NULL ELSE ROW (kvp_fom, kvp_tom)::periode_open END,
    intro_periode                 = CASE WHEN intro_fom IS NULL AND intro_tom IS NULL THEN NULL ELSE ROW (intro_fom, intro_tom)::periode_open END,
    institusjon_periode           = CASE WHEN institusjon_fom IS NULL AND institusjon_tom IS NULL THEN NULL ELSE ROW (institusjon_fom, institusjon_tom)::periode_open END,
    sykepenger_periode            = CASE WHEN sykepenger_fom IS NULL AND sykepenger_tom IS NULL THEN NULL ELSE ROW (sykepenger_fom, sykepenger_tom)::periode_open END,
    supplerende_alder_periode     = CASE WHEN supplerende_alder_fom IS NULL AND supplerende_alder_tom IS NULL THEN NULL ELSE ROW (supplerende_alder_fom, supplerende_alder_tom)::periode_open END,
    supplerende_flyktning_periode = CASE WHEN supplerende_flyktning_fom IS NULL AND supplerende_flyktning_tom IS NULL THEN NULL ELSE ROW (supplerende_flyktning_fom, supplerende_flyktning_tom)::periode_open END,
    jobbsjansen_periode           = CASE WHEN jobbsjansen_fom IS NULL AND jobbsjansen_tom IS NULL THEN NULL ELSE ROW (jobbsjansen_fom, jobbsjansen_tom)::periode_open END,
    gjenlevendepensjon_periode    = CASE WHEN gjenlevendepensjon_fom IS NULL AND gjenlevendepensjon_tom IS NULL THEN NULL ELSE ROW (gjenlevendepensjon_fom, gjenlevendepensjon_tom)::periode_open END,
    trygd_og_pensjon_periode      = CASE WHEN trygd_og_pensjon_fom IS NULL AND trygd_og_pensjon_tom IS NULL THEN NULL ELSE ROW (trygd_og_pensjon_fom, trygd_og_pensjon_tom)::periode_open END,
    manuelt_satt_soknadsperiode   = CASE WHEN manuelt_satt_soknadsperiode_fra_og_med IS NULL THEN NULL ELSE ROW (manuelt_satt_soknadsperiode_fra_og_med, manuelt_satt_soknadsperiode_til_og_med)::periode END;
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
    ADD COLUMN deltakelse periode;
UPDATE søknadstiltak
SET deltakelse = ROW (deltakelse_fra_og_med, deltakelse_til_og_med)::periode;
ALTER TABLE søknadstiltak
    ALTER COLUMN deltakelse SET NOT NULL,
    DROP COLUMN deltakelse_fra_og_med,
    DROP COLUMN deltakelse_til_og_med;

-- Tiltaksdeltakerhendelser kan mangle den ene eller begge endene, derav periode_open.
ALTER TABLE tiltaksdeltaker_kafka
    ADD COLUMN deltakelse periode_open;
UPDATE tiltaksdeltaker_kafka
SET deltakelse = ROW (deltakelse_fra_og_med, deltakelse_til_og_med)::periode_open
WHERE deltakelse_fra_og_med IS NOT NULL
   OR deltakelse_til_og_med IS NOT NULL;
ALTER TABLE tiltaksdeltaker_kafka
    DROP COLUMN deltakelse_fra_og_med,
    DROP COLUMN deltakelse_til_og_med;
