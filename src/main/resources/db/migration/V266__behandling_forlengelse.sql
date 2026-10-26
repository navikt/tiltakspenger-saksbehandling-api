-- Vedtaket en forlengelse forlenger, første dag i forlengelsen, og om saksbehandler har valgt barnetillegget selv.
-- Settes bare for behandlinger med resultat FORLENGELSE.
ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS forlengelse jsonb;
