CREATE TABLE meldeperiode (
    id varchar PRIMARY KEY,
    hendelse_id varchar NOT NULL,
    versjon INTEGER NOT NULL,
    sak_id UUID NOT NULL,
    opprettet TIMESTAMPTZ NOT NULL,
    fra_og_med DATE NOT NULL,
    til_og_med DATE  NOT NULL,
    antall_dager_for_periode INTEGER NOT NULL,
    gir_rett JSONB NOT NULL
);
CREATE INDEX idx_meldeperiode_sak_id ON meldeperiode(sak_id);
CREATE INDEX idx_meldeperiode_hendelse_id ON meldeperiode(hendelse_id);
CREATE INDEX idx_meldeperiode_id_versjon ON meldeperiode(id, versjon);
CREATE INDEX idx_meldeperiode_periode ON meldeperiode(fra_og_med, til_og_med);
