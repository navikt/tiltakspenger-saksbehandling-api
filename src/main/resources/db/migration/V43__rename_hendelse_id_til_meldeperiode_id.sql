ALTER TABLE meldeperiode RENAME COLUMN id to kjede_id;
ALTER TABLE meldeperiode RENAME COLUMN hendelse_id to id;
ALTER TABLE meldekort_bruker RENAME COLUMN meldeperiode_id to meldeperiode_kjede_id;
ALTER TABLE meldekort_bruker RENAME COLUMN meldeperiode_hendelse_id to meldeperiode_id;

ALTER TABLE meldekortbehandling RENAME COLUMN meldeperiode_id to meldeperiode_kjede_id;
ALTER TABLE meldekortbehandling RENAME COLUMN meldeperiode_hendelse_id to meldeperiode_id;