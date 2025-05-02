ALTER TABLE statistikk_utbetaling ADD COLUMN IF NOT EXISTS vedtak_id varchar;
ALTER TABLE statistikk_utbetaling ADD COLUMN IF NOT EXISTS opprettet TIMESTAMPTZ;
ALTER TABLE statistikk_utbetaling ADD COLUMN IF NOT EXISTS sist_endret TIMESTAMPTZ;

ALTER TABLE statistikk_utbetaling DROP COLUMN IF EXISTS arsak;
