ALTER TABLE tiltaksdeltaker_kafka REPLICA IDENTITY FULL;

ALTER TABLE tiltaksdeltaker_kafka DROP CONSTRAINT tiltaksdeltaker_kafka_pkey;

ALTER TABLE tiltaksdeltaker_kafka RENAME COLUMN id TO deltaker_id;

ALTER TABLE tiltaksdeltaker_kafka ADD COLUMN hendelse_id varchar;

UPDATE tiltaksdeltaker_kafka SET hendelse_id = 'tiltaksdeltakerhendelse_' || replace(gen_random_uuid()::text, '-', '') WHERE hendelse_id IS NULL;

ALTER TABLE tiltaksdeltaker_kafka ALTER COLUMN hendelse_id SET NOT NULL;

ALTER TABLE tiltaksdeltaker_kafka ADD PRIMARY KEY (hendelse_id);

ALTER TABLE tiltaksdeltaker_kafka REPLICA IDENTITY DEFAULT;

CREATE INDEX idx_tiltaksdeltaker_kafka_deltaker_id ON tiltaksdeltaker_kafka (deltaker_id);

