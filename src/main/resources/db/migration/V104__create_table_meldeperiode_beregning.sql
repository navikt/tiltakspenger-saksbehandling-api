CREATE TABLE meldeperiode_beregning
(
    id                     varchar PRIMARY KEY,
    meldeperiode_kjede_id  varchar NOT NULL,
    meldekort_id           varchar NOT NULL REFERENCES meldekortbehandling (id),
    dager                  jsonb   NOT NULL,
    iverksatt              TIMESTAMPTZ,

    kilde                  varchar NOT NULL,
    beregnet_meldekort_id  varchar REFERENCES meldekortbehandling (id),
    beregnet_behandling_id varchar REFERENCES behandling (id),

    CHECK (
        (kilde = 'BEHANDLING' AND beregnet_meldekort_id IS NULL AND beregnet_behandling_id IS NOT NULL) OR
        (kilde = 'MELDEKORT' AND beregnet_meldekort_id IS NOT NULL AND beregnet_behandling_id IS NULL)
        )
);

CREATE INDEX idx_meldeperiode_beregning_kjede_id ON meldeperiode_beregning (meldeperiode_kjede_id);
CREATE INDEX idx_meldeperiode_beregning_meldekort_id ON meldeperiode_beregning (meldekort_id);
CREATE INDEX idx_meldeperiode_beregning_beregnet_meldekort_id ON meldeperiode_beregning (beregnet_meldekort_id);
CREATE INDEX idx_meldeperiode_beregning_beregnet_behandling_id ON meldeperiode_beregning (beregnet_behandling_id);
