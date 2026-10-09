-- mottatt er når søknaden kom inn til Nav, opprettet er når vi registrerte den.
-- Før denne migreringen lå det første i opprettet og det andre i tidsstempel_hos_oss.
ALTER TABLE søknad
    ADD COLUMN mottatt TIMESTAMPTZ;

-- Høyresidene leses fra raden slik den var før oppdateringen, så de to verdiene byttes uten mellomlagring.
UPDATE søknad
SET mottatt   = opprettet,
    opprettet = tidsstempel_hos_oss;

ALTER TABLE søknad
    ALTER COLUMN mottatt SET NOT NULL;

ALTER TABLE søknad
    DROP COLUMN tidsstempel_hos_oss;