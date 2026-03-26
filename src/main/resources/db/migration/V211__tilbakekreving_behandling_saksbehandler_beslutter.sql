ALTER TABLE tilbakekreving_behandling
    ADD COLUMN IF NOT EXISTS saksbehandler_ident VARCHAR NULL;

ALTER TABLE tilbakekreving_behandling
    ADD COLUMN IF NOT EXISTS beslutter_ident VARCHAR NULL;

ALTER TABLE tilbakekreving_behandling
    ADD CONSTRAINT chk_saksbehandler_ikke_lik_beslutter
        CHECK (saksbehandler_ident IS DISTINCT FROM beslutter_ident OR saksbehandler_ident IS NULL);
