-- Saksbehandlers begrunnelse for at rammebehandlingen er høyt prioritert.
-- Null betyr at behandlingen ikke er prioritert.
ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS prioritering_begrunnelse text;
