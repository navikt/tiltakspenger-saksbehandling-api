ALTER TABLE behandling
  ADD COLUMN IF NOT EXISTS sendt_til_datadeling timestamptz NULL;

ALTER TABLE rammevedtak
  ADD COLUMN IF NOT EXISTS sendt_til_datadeling timestamptz NULL;
