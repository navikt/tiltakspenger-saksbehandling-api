ALTER TABLE meldekortvedtak
  ADD COLUMN IF NOT EXISTS sendt_til_datadeling timestamptz NULL;
