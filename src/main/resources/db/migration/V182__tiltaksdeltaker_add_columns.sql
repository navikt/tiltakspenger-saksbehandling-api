ALTER TABLE tiltaksdeltaker ADD COLUMN IF NOT EXISTS tiltakstype VARCHAR;
ALTER TABLE tiltaksdeltaker ADD COLUMN IF NOT EXISTS utdatert_ekstern_id VARCHAR default null;
