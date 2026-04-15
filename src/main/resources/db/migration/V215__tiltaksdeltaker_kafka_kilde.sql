ALTER TABLE tiltaksdeltaker_kafka
    ADD COLUMN IF NOT EXISTS kilde varchar;
ALTER TABLE tiltaksdeltaker_kafka
    ADD COLUMN IF NOT EXISTS behandlet_tidspunkt TIMESTAMPTZ;

UPDATE tiltaksdeltaker_kafka
SET behandlet_tidspunkt = sist_oppdatert
WHERE behandlet_tidspunkt IS NULL
  AND (oppgave_id IS NOT NULL OR behandling_id IS NOT NULL);

