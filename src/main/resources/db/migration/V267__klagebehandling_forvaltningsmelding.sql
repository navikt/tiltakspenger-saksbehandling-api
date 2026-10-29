-- Forvaltningsmelding (informasjon om saksbehandlingstid) som saksbehandler har sendt til bruker fra en klagebehandling.
-- Det kan bare sendes én per klagebehandling, så den lagres på klagebehandlingen.
-- Journalføring og distribusjon fylles inn av jobbene, og forvaltningsmelding_brev_metadata er råresponsen fra dokarkiv.
ALTER TABLE klagebehandling
    ADD COLUMN IF NOT EXISTS forvaltningsmelding jsonb,
    ADD COLUMN IF NOT EXISTS forvaltningsmelding_brev_metadata jsonb;
