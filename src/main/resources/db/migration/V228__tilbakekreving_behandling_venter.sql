ALTER TABLE tilbakekreving_behandling
    ADD COLUMN IF NOT EXISTS venter JSONB NULL;

