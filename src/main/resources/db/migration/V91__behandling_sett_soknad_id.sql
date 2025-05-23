-- Copy relation from søknad.behandling_id to behandling.søknad_id
UPDATE behandling b
SET soknad_id = s.id
FROM søknad s
WHERE s.behandling_id = b.id;

ALTER TABLE behandling
    ADD CONSTRAINT behandling_soknad_id_fkey
        FOREIGN KEY (soknad_id) REFERENCES søknad (id);
