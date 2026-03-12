ALTER TABLE tilbakekreving_hendelse
    ADD COLUMN IF NOT EXISTS ekstern_behandling_id VARCHAR NULL,
    ADD COLUMN IF NOT EXISTS tilbake_behandling_id VARCHAR NULL,
    ADD COLUMN IF NOT EXISTS sak_opprettet TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS varsel_sendt DATE NULL,
    ADD COLUMN IF NOT EXISTS feilutbetalt_beløp NUMERIC NULL
;
