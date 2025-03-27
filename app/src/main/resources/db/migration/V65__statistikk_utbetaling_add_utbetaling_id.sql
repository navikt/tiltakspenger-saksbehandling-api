ALTER TABLE statistikk_utbetaling ADD COLUMN IF NOT EXISTS utbetaling_id varchar;

update statistikk_utbetaling set utbetaling_id=RIGHT(id, 15);