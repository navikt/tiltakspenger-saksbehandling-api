ALTER TABLE behandling ADD COLUMN IF NOT EXISTS fritekst_vedtaksbrev VARCHAR;
ALTER TABLE behandling ADD COLUMN IF NOT EXISTS begrunnelse_vilkårsvurdering VARCHAR;
ALTER TABLE behandling ADD COLUMN IF NOT EXISTS saksopplysninger JSONB;
ALTER TABLE behandling ALTER COLUMN vilkårssett DROP NOT NULL;