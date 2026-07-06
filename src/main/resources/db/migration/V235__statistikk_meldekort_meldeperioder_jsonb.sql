-- Migrerer statistikk_meldekort fra å lagre én meldeperiode i flate kolonner
-- (meldeperiode_kjede_id, fra_og_med, til_og_med, meldekortdager) til en jsonb-array
-- meldeperioder, slik at vi kan lagre flere meldeperioder per meldekortbehandling.
--
-- De gamle kolonnene beholdes foreløpig for bakoverkompatibilitet; de fjernes i en
-- senere migrering når konsumentene har tatt i bruk meldeperioder.

ALTER TABLE statistikk_meldekort ADD COLUMN meldeperioder jsonb;

UPDATE statistikk_meldekort
SET meldeperioder = jsonb_build_array(
    jsonb_build_object(
        'fraOgMed', fra_og_med,
        'tilOgMed', til_og_med,
        'meldeperiodeKjedeId', meldeperiode_kjede_id,
        'meldekortdager', meldekortdager
    )
);

ALTER TABLE statistikk_meldekort ALTER COLUMN meldeperioder SET NOT NULL;
