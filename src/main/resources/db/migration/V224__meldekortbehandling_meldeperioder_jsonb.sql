-- Migrerer meldekortbehandling fra å lagre én meldeperiodebehandling i flate kolonner
-- (meldeperiode_id, meldeperiode_kjede_id, brukers_meldekort_id, meldekortdager) til en jsonb-array
-- meldeperioder, slik at vi etterhvert kan støtte flere meldeperiodebehandlinger per behandling.
-- Domenet håndhever fortsatt at det kun finnes én meldeperiodebehandling per behandling.
--
-- For å kunne rulle tilbake beholder vi de gamle kolonnene foreløpig - de gjøres bare nullable.
-- Nye rader vil ha null i de gamle kolonnene; de fjernes i en senere migrering når vi er trygge.

ALTER TABLE meldekortbehandling ADD COLUMN meldeperioder jsonb;

UPDATE meldekortbehandling
SET meldeperioder = jsonb_build_array(
    jsonb_build_object(
        'meldeperiodeId', meldeperiode_id,
        'kjedeId', meldeperiode_kjede_id,
        'brukersMeldekortId', brukers_meldekort_id,
        'dager', meldekortdager
    )
);

ALTER TABLE meldekortbehandling ALTER COLUMN meldeperioder SET NOT NULL;

-- Gjør de gamle kolonnene nullable slik at nye rader (som kun skriver til meldeperioder) ikke feiler.
ALTER TABLE meldekortbehandling
    ALTER COLUMN meldeperiode_id DROP NOT NULL,
    ALTER COLUMN meldeperiode_kjede_id DROP NOT NULL,
    ALTER COLUMN meldekortdager DROP NOT NULL;

ALTER TABLE meldekortbehandling DROP CONSTRAINT meldekort_meldeperiode_id_fkey;

-- Nye indekser basert på jsonb-feltene. Så lenge vi håndhever én meldeperiodebehandling per
-- behandling kan vi bruke btree på element 0; må eventuelt erstattes med GIN når vi får støtte
-- for flere meldeperioder per behandling.
--
-- Vi beholder de gamle indeksene (idx_meldekortbehandling_automatisk_brukersmeldekort_id_unique
-- og idx_meldekortbehandling_kjede_sist_endret) inntil videre for rollback-trygghet. De vil ikke
-- inneholde nye rader siden de gamle kolonnene blir null, men det er greit - alle nye spørringer
-- bruker indeksene under.
CREATE UNIQUE INDEX idx_meldekortbehandling_auto_brukersmeldekort_jsonb_unique
    ON meldekortbehandling ((meldeperioder -> 0 ->> 'brukersMeldekortId'))
    WHERE status = 'AUTOMATISK_BEHANDLET';

CREATE INDEX idx_meldekortbehandling_kjede_jsonb_sist_endret
    ON meldekortbehandling (sak_id, ((meldeperioder -> 0 ->> 'kjedeId')), sist_endret);

