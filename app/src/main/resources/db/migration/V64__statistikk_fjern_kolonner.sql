ALTER TABLE statistikk_sak DROP COLUMN IF EXISTS sakUtland;
ALTER TABLE statistikk_sak DROP COLUMN IF EXISTS ansvarligEnhet;

ALTER TABLE statistikk_utbetaling DROP COLUMN IF EXISTS beløp_beskrivelse;

ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS opplysning;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS oppfølging_enhet_kode;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS oppfølging_enhet_navn;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS beslutning_enhet_kode;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS beslutning_enhet_navn;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS tilhørighet_enhet_kode;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS tilhørighet_enhet_navn;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS vilkår_id;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS vilkår_type;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS vilkår_status;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS lovparagraf;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS beskrivelse;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS gyldig_fra_dato_vilkår;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS gyldig_til_dato_vilkår;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS tiltak_id;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS tiltak_type;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS tiltak_beskrivelse;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS tiltak_dato;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS gyldig_fra_dato_tiltak;
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS gyldig_til_dato_tiltak;