-- 1337: Lagt til kolonne for å kunne lagre årsaksgrunn for behandling ved revurdering til stans/avslag
ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS valgt_hjemmel_har_ikke_rettighet JSONB;
