-- Begrunnelsen for at en korrigering fra bruker, som ikke kunne behandles automatisk, er høyt prioritert.
-- Null betyr at meldekortet ikke er prioritert.
ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS prioritering_begrunnelse text;
