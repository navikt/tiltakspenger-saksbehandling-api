ALTER TABLE statistikk_sak RENAME COLUMN søknadsformat TO soknadsformat;
ALTER TABLE statistikk_sak RENAME COLUMN tilbakekrevingsbeløp TO tilbakekrevingsbelop;

ALTER TABLE statistikk_sak_vilkar RENAME COLUMN vilkår TO vilkar;

ALTER TABLE statistikk_stonad RENAME COLUMN søknad_id TO soknad_id;
ALTER TABLE statistikk_stonad RENAME COLUMN søknad_dato TO soknad_dato;
ALTER TABLE statistikk_stonad RENAME COLUMN gyldig_fra_dato_søknad TO gyldig_fra_dato_soknad;
ALTER TABLE statistikk_stonad RENAME COLUMN gyldig_til_dato_søknad TO gyldig_til_dato_soknad;

ALTER TABLE statistikk_utbetaling RENAME COLUMN beløp TO belop;
ALTER TABLE statistikk_utbetaling RENAME COLUMN ordinær_beløp TO ordinar_belop;
ALTER TABLE statistikk_utbetaling RENAME COLUMN barnetillegg_beløp TO barnetillegg_belop;
ALTER TABLE statistikk_utbetaling RENAME COLUMN årsak TO arsak;