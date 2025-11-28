ALTER TABLE søknad
    ADD COLUMN IF NOT EXISTS har_sokt_paa_tiltak_type      VARCHAR,
    ADD COLUMN IF NOT EXISTS har_sokt_om_barnetillegg_type VARCHAR;

update søknad s
set har_sokt_paa_tiltak_type = 'JA'
where exists (select 1 from søknadstiltak st where st.søknad_id = s.id);

update søknad
set har_sokt_paa_tiltak_type = 'NEI'
where har_sokt_paa_tiltak_type is null;

update søknad s
set har_sokt_om_barnetillegg_type = 'JA'
where exists (select 1 from søknad_barnetillegg sb where sb.søknad_id = s.id);

update søknad
set har_sokt_om_barnetillegg_type = 'NEI'
where har_sokt_om_barnetillegg_type is null;
