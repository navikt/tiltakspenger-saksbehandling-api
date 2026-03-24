ALTER TABLE tilbakekreving_behandling
    ADD COLUMN IF NOT EXISTS saksbehandler_ident VARCHAR NULL;

ALTER TABLE tilbakekreving_behandling
    ADD COLUMN IF NOT EXISTS beslutter_ident VARCHAR NULL;

