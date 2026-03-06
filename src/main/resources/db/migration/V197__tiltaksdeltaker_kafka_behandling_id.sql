ALTER TABLE tiltaksdeltaker_kafka
    ADD COLUMN IF NOT EXISTS behandling_id varchar NULL REFERENCES behandling (id)