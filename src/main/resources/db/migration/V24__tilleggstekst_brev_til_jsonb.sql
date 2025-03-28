ALTER TABLE behandling ADD COLUMN temp_jsonb_col jsonb;
UPDATE behandling SET temp_jsonb_col = tilleggstekst_brev::jsonb;
ALTER TABLE behandling DROP COLUMN tilleggstekst_brev;
ALTER TABLE behandling RENAME COLUMN temp_jsonb_col TO tilleggstekst_brev;
