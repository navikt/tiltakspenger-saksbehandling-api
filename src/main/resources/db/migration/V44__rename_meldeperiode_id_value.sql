ALTER TABLE meldekort_bruker DROP CONSTRAINT meldekort_bruker_meldeperiode_hendelse_id_fkey;

UPDATE meldeperiode
  SET id = REPLACE(id, 'hendelse_', 'meldeperiode_')
  WHERE id LIKE 'hendelse_%';

UPDATE meldekort_bruker
  SET meldeperiode_id = REPLACE(meldeperiode_id, 'hendelse_', 'meldeperiode_')
  WHERE meldeperiode_id LIKE 'hendelse_%';

UPDATE meldekortbehandling
  SET meldeperiode_id = REPLACE(meldeperiode_id, 'hendelse_', 'meldeperiode_')
  WHERE meldeperiode_id LIKE 'hendelse_%';

ALTER TABLE meldekort_bruker
  ADD CONSTRAINT meldekort_bruker_meldeperiode_id_fkey
  FOREIGN KEY (meldeperiode_id)
  REFERENCES meldeperiode(id);

ALTER TABLE meldekortbehandling
  ADD CONSTRAINT meldekort_meldeperiode_id_fkey
  FOREIGN KEY (meldeperiode_id)
  REFERENCES meldeperiode(id);