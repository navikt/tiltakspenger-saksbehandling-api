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

-- De gamle fra/til-kolonnene beholdes her og droppes senere, slik at denne migreringen er ikke-destruktiv.
-- De gamle kolonnene skrives ikke lenger av koden, så de som var NOT NULL må bli nullable.
-- Det skjer i V245: rammevedtak har en DEFERRABLE INITIALLY DEFERRED fremmednøkkel (utbetaling_id), så en UPDATE som treffer rader legger utsatte trigger-eventer i kø til commit, og en ALTER i samme transaksjon feiler med "pending trigger events".

-- behandling: kolonnene het virkningsperiode_*, men innholdet er vedtaksperioden (jf. TODO i RammebehandlingDb).
ALTER TABLE behandling
    ADD COLUMN vedtaksperiode periode;
UPDATE behandling
SET vedtaksperiode = ROW (virkningsperiode_fra_og_med, virkningsperiode_til_og_med)::periode
WHERE virkningsperiode_fra_og_med IS NOT NULL;

ALTER TABLE meldekortbehandling
    ADD COLUMN periode periode;
UPDATE meldekortbehandling
SET periode = ROW (fra_og_med, til_og_med)::periode;

ALTER TABLE meldeperiode
    ADD COLUMN periode periode;
UPDATE meldeperiode
SET periode = ROW (fra_og_med, til_og_med)::periode;

ALTER TABLE rammevedtak
    ADD COLUMN periode periode;
UPDATE rammevedtak
SET periode = ROW (fra_og_med, til_og_med)::periode;

-- Statistikk-tabellene (statistikk_meldekort, statistikk_sak, statistikk_stonad) beholder fra/til-kolonnene sine.
-- De har eksterne konsumenter som ikke kjenner de egendefinerte periode-typene våre.

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

ALTER TABLE søknadstiltak
    ADD COLUMN deltakelse_periode periode;
UPDATE søknadstiltak
SET deltakelse_periode = ROW (deltakelse_fra_og_med, deltakelse_til_og_med)::periode;

-- Tiltaksdeltakerhendelser kan mangle den ene eller begge endene, derav periode_open.
ALTER TABLE tiltaksdeltaker_kafka
    ADD COLUMN deltakelse_periode periode_open;
UPDATE tiltaksdeltaker_kafka
SET deltakelse_periode = ROW (deltakelse_fra_og_med, deltakelse_til_og_med)::periode_open
WHERE deltakelse_fra_og_med IS NOT NULL
   OR deltakelse_til_og_med IS NOT NULL;
