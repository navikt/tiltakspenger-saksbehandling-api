ALTER TABLE meldekortbehandling
  ADD COLUMN IF NOT EXISTS behandling_sendt_til_datadeling timestamptz NULL;
