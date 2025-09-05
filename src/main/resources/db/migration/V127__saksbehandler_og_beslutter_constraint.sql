ALTER TABLE behandling
    ADD CONSTRAINT saksbehandling_beslutter_not_equal CHECK (saksbehandler <> beslutter);

-- saksbehandler og beslutter er lik ved automatisk behandling
ALTER TABLE meldekortbehandling
    ADD CONSTRAINT saksbehandling_beslutter_not_equal CHECK (status = 'AUTOMATISK_BEHANDLET' OR (saksbehandler <> beslutter));
