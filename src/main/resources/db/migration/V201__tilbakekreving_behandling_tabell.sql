CREATE TABLE tilbakekreving_behandling (
    id VARCHAR PRIMARY KEY,
    sak_id VARCHAR NOT NULL REFERENCES sak (id),
    opprettet TIMESTAMPTZ NOT NULL,
    utbetaling_id VARCHAR NOT NULL UNIQUE REFERENCES utbetaling(id),
    status VARCHAR NOT NULL,
    url VARCHAR NOT NULL,
    kravgrunnlag_periode_fom DATE NOT NULL,
    kravgrunnlag_periode_tom DATE NOT NULL,
    totalt_feilutbetalt_beløp NUMERIC NOT NULL,
    varsel_sendt DATE NULL,
    tilbake_behandling_id VARCHAR NOT NULL
);

CREATE INDEX idx_tilbakekreving_behandling_sak_id ON tilbakekreving_behandling (sak_id);
