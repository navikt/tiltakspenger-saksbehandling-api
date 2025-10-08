ALTER TABLE meldekortbehandling
  ADD COLUMN IF NOT EXISTS sendt_til_datadeling timestamptz NULL;
