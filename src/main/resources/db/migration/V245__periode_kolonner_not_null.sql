-- V244 fylte de nye periode-kolonnene; denne migreringen strammer inn.
-- Den må være en egen migrering fordi rammevedtak har en DEFERRABLE INITIALLY DEFERRED fremmednøkkel (utbetaling_id): en UPDATE som treffer rader legger utsatte trigger-eventer i kø til commit, og en ALTER i samme transaksjon feiler da med "pending trigger events".

ALTER TABLE meldekortbehandling
    ALTER COLUMN periode SET NOT NULL,
    ALTER COLUMN fra_og_med DROP NOT NULL,
    ALTER COLUMN til_og_med DROP NOT NULL;

ALTER TABLE meldeperiode
    ALTER COLUMN periode SET NOT NULL,
    ALTER COLUMN fra_og_med DROP NOT NULL,
    ALTER COLUMN til_og_med DROP NOT NULL;

ALTER TABLE rammevedtak
    ALTER COLUMN periode SET NOT NULL,
    ALTER COLUMN fra_og_med DROP NOT NULL,
    ALTER COLUMN til_og_med DROP NOT NULL;

ALTER TABLE søknadstiltak
    ALTER COLUMN deltakelse_periode SET NOT NULL,
    ALTER COLUMN deltakelse_fra_og_med DROP NOT NULL;
