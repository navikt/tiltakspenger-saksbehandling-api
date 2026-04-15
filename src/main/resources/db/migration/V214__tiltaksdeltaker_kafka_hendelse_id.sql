
ALTER TABLE tiltaksdeltaker_kafka
    REPLICA IDENTITY FULL;

ALTER TABLE tiltaksdeltaker_kafka
    DROP CONSTRAINT IF EXISTS tiltaksdeltaker_kafka_pkey;

ALTER TABLE tiltaksdeltaker_kafka
    RENAME COLUMN id TO deltaker_id;

ALTER TABLE tiltaksdeltaker_kafka
    ADD COLUMN IF NOT EXISTS hendelse_id varchar;

-- Generate valid ULID-formatted hendelse_id for existing rows
-- A ULID is 26 chars of Crockford Base32. We use a temporary function to encode random bytes.
CREATE OR REPLACE FUNCTION pg_temp.bytes_to_crockford(data bytea) RETURNS text AS
$$
DECLARE
    alphabet text := '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
    result   text := '';
    bits     int  := 0;
    buffer   int  := 0;
    i        int;
    byte     int;
BEGIN
    FOR i IN 0..length(data) - 1
        LOOP
            byte := get_byte(data, i);
            buffer := (buffer << 8) | byte;
            bits := bits + 8;
            WHILE bits >= 5
                LOOP
                    bits := bits - 5;
                    result := result || substr(alphabet, ((buffer >> bits) & 31) + 1, 1);
                END LOOP;
        END LOOP;
    IF bits > 0 THEN
        result := result || substr(alphabet, ((buffer << (5 - bits)) & 31) + 1, 1);
    END IF;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

UPDATE tiltaksdeltaker_kafka
SET hendelse_id = 'tiltaksdeltakerhendelse_' || pg_temp.bytes_to_crockford(uuid_send(gen_random_uuid()))
WHERE hendelse_id IS NULL;

ALTER TABLE tiltaksdeltaker_kafka
    ALTER COLUMN hendelse_id SET NOT NULL;

ALTER TABLE tiltaksdeltaker_kafka
    ADD PRIMARY KEY (hendelse_id);

ALTER TABLE tiltaksdeltaker_kafka
    REPLICA IDENTITY DEFAULT;

CREATE INDEX IF NOT EXISTS idx_tiltaksdeltaker_kafka_deltaker_id ON tiltaksdeltaker_kafka (deltaker_id);
