ALTER TABLE behandling ADD COLUMN IF NOT EXISTS har_valgt_skal_omgjøre_hele_vedtaksperioden BOOLEAN default true;
ALTER TABLE behandling ADD COLUMN IF NOT EXISTS valgt_omgjøringsperiode periode null;