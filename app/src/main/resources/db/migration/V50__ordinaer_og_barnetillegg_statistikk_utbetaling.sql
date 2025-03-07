ALTER TABLE statistikk_utbetaling
    ADD COLUMN IF NOT EXISTS ordinær_beløp INTEGER;
ALTER TABLE statistikk_utbetaling
    ADD COLUMN IF NOT EXISTS barnetillegg_beløp INTEGER DEFAULT 0;
UPDATE statistikk_utbetaling
set ordinær_beløp = beløp
where statistikk_utbetaling.ordinær_beløp is null
  and beløp is not null;

