ALTER TABLE meldeperiode
ADD CONSTRAINT fk_meldeperiode_sak_id
FOREIGN KEY (sak_id) REFERENCES sak(id)