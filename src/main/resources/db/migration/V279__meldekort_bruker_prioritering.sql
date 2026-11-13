-- Begrunnelse for at et korrigert meldekort fra bruker er høyt prioritert i benken.
-- Settes ved mottak dersom utbetalingen av en tidligere behandling av meldeperioden ennå ikke er iverksatt av oppdragssystemet.
-- Null betyr at meldekortet ikke er prioritert.
ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS prioritering_begrunnelse text;
