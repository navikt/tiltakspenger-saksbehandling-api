-- 1302: Legger til journalpost_id for å kunne opprette oppgaver for mottatt meldekort og oppgave_id for å kunne ferdigstille
-- tilknyttet oppgave når et meldekort blir iverksatt.
ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS journalpost_id VARCHAR,
    ADD COLUMN IF NOT EXISTS oppgave_id     VARCHAR;
