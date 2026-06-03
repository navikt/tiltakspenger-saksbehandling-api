-- Erstattet av meldeperioder, se V224
ALTER TABLE meldekortbehandling
    DROP COLUMN meldeperiode_id,
    DROP COLUMN meldeperiode_kjede_id,
    DROP COLUMN meldekortdager,
    DROP COLUMN brukers_meldekort_id;
