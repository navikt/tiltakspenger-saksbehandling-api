ALTER TABLE behandling
  ADD COLUMN IF NOT EXISTS sendt_til_beslutning timestamptz NULL;

ALTER TABLE meldekort
  ADD COLUMN IF NOT EXISTS sendt_til_beslutning timestamptz NULL;
