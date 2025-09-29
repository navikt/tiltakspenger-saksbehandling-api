-- For å kunne håndtere og lagre papirsøknad må feltene kunne være null, dvs at bruker ikke har oppgitt et svar.
ALTER TABLE søknad
    ALTER COLUMN kvp_type DROP NOT NULL,
    ALTER COLUMN intro_type DROP NOT NULL,
    ALTER COLUMN institusjon_type DROP NOT NULL,
    ALTER COLUMN sykepenger_type DROP NOT NULL,
    ALTER COLUMN supplerende_alder_type DROP NOT NULL,
    ALTER COLUMN supplerende_flyktning_type DROP NOT NULL,
    ALTER COLUMN jobbsjansen_type DROP NOT NULL,
    ALTER COLUMN alderspensjon_type DROP NOT NULL,
    ALTER COLUMN trygd_og_pensjon_type DROP NOT NULL,
    ALTER COLUMN etterlonn_type DROP NOT NULL,
    ALTER COLUMN gjenlevendepensjon_type DROP NOT NULL
;
