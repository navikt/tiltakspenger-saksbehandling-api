update meldekortbehandling
set status = 'UNDER_BEHANDLING'
WHERE status = 'KLAR_TIL_BEHANDLING'
  and saksbehandler is not null;