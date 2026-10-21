-- Endringer beslutter har gjort i saksbehandlers valg, mens behandlingen er under beslutning.
-- De gjeldende verdiene ligger fortsatt i behandlingens egne kolonner, f.eks. skal_journalfore_notat.
-- Denne kolonnen er historikken: saksbehandlers valg, beslutters valg og tidspunktet for endringen, per felt.
-- Hvem som endret, er behandlingens beslutter.
-- Et tomt objekt betyr at beslutter ikke har endret noe.
ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS beslutter_endringer jsonb NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS beslutter_endringer jsonb NOT NULL DEFAULT '{}'::jsonb;
