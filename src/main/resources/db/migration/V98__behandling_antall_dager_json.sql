ALTER TABLE behandling
    ADD COLUMN antall_dager_per_meldeperiode_json JSONB;

UPDATE behandling
SET antall_dager_per_meldeperiode_json =
        jsonb_build_array(
                jsonb_build_object(
                        'antallDagerPerMeldeperiode', antall_dager_per_meldeperiode,
                        'periode', jsonb_build_object(
                                'fra_og_med', virkningsperiode_fra_og_med,
                                'til_og_med', virkningsperiode_til_og_med
                                   )
                )
        )
WHERE antall_dager_per_meldeperiode IS NOT NULL;