CREATE TABLE meldeperiode (
    hendelse_id varchar PRIMARY KEY,
    id varchar NOT NULL,
    versjon INTEGER NOT NULL,
    sak_id varchar NOT NULL,
    opprettet TIMESTAMPTZ NOT NULL,
    fra_og_med DATE NOT NULL,
    til_og_med DATE NOT NULL,
    antall_dager_for_periode INTEGER NOT NULL,
    gir_rett JSONB NOT NULL,
    sendt_til_meldekort_api timestamptz NULL,

    CONSTRAINT unique_id_opprettet_sak_id UNIQUE (id, sak_id, opprettet),
    CONSTRAINT unique_id_versjon_sak_id UNIQUE (id, versjon, sak_id)
);
CREATE INDEX idx_meldeperiode_sak_id ON meldeperiode(sak_id);
CREATE INDEX idx_meldeperiode_id ON meldeperiode(id);
CREATE INDEX idx_meldeperiode_id_versjon ON meldeperiode(id, versjon);
CREATE INDEX idx_meldeperiode_periode ON meldeperiode(fra_og_med, til_og_med);
ALTER TABLE meldekort DROP COLUMN IF EXISTS sendt_til_meldekort_api;
ALTER TABLE meldekort ADD COLUMN IF NOT EXISTS meldeperiode_hendelse_id varchar NOT NULL default 'defaultid';
