CREATE TABLE meldekort_bruker (
  id varchar PRIMARY KEY,
  meldeperiode_hendelse_id varchar NOT NULL REFERENCES meldeperiode(hendelse_id),
  meldeperiode_id varchar NOT NULL,
  meldeperiode_versjon integer NOT NULL,
  sak_id varchar NOT NULL REFERENCES sak(id),
  mottatt TIMESTAMPTZ NOT NULL,
  dager jsonb NOT NULL,

  FOREIGN KEY (meldeperiode_id, meldeperiode_versjon, sak_id)
    REFERENCES meldeperiode(id, versjon, sak_id)
);

CREATE INDEX idx_meldekort_bruker_sak_id ON meldekort_bruker(sak_id);
CREATE INDEX idx_meldekort_bruker_meldeperiode_hendelse_id ON meldekort_bruker(meldeperiode_hendelse_id);