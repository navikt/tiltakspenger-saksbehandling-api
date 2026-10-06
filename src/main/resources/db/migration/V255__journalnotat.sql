-- Saksbehandlers valg om notatet for behandlingen skal journalføres.
-- Behandlinger fra før valget fantes får false, og journalføres ikke.
ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS skal_journalfore_notat boolean NOT NULL DEFAULT FALSE;

ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS skal_journalfore_notat boolean NOT NULL DEFAULT FALSE;

-- Kvittering for journalført notat (journalpostId og journalføringstidspunkt).
-- Null fram til notatet er journalført, og forblir null når notatet ikke skal journalføres.
ALTER TABLE rammevedtak
    ADD COLUMN IF NOT EXISTS journalføringsnotat jsonb;

ALTER TABLE meldekortvedtak
    ADD COLUMN IF NOT EXISTS journalføringsnotat jsonb;
