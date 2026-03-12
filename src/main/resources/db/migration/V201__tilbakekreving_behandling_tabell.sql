CREATE TABLE tilbakekreving_behandling (
    id VARCHAR PRIMARY KEY,
    sak_id VARCHAR NOT NULL REFERENCES sak (id),
    utbetaling_id VARCHAR NOT NULL REFERENCES utbetaling(id),
    tilbake_behandling_id VARCHAR NOT NULL,
    opprettet TIMESTAMPTZ NOT NULL,
    status VARCHAR NOT NULL,
    url VARCHAR NOT NULL,
    kravgrunnlag_periode PERIODE NOT NULL,
    totalt_feilutbetalt_beløp NUMERIC NOT NULL,
    varsel_sendt DATE NULL
);

CREATE INDEX idx_tilbakekreving_behandling_sak_id ON tilbakekreving_behandling (sak_id);
CREATE INDEX idx_tilbakekreving_behandling_utbetaling_id ON tilbakekreving_behandling (utbetaling_id);
CREATE INDEX idx_tilbakekreving_behandling_tilbake_behandling_id ON tilbakekreving_behandling (tilbake_behandling_id);
