-- Saksbehandlers begrunnelse for at meldekortbehandlingen er høyt prioritert.
-- Null betyr at behandlingen ikke er prioritert.
ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS prioritering_begrunnelse text;
