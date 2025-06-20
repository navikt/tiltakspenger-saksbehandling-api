UPDATE behandling SET antall_dager_per_meldeperiode_json = null where 1=1;

UPDATE behandling
SET antall_dager_per_meldeperiode_json =
        jsonb_build_array(
                jsonb_build_object(
                        'antallDagerPerMeldeperiode', antall_dager_per_meldeperiode::int,
                        'periode', jsonb_build_object(
                                'fra_og_med', virkningsperiode_fra_og_med,
                                'til_og_med', virkningsperiode_til_og_med
                                   )
                )
        )
WHERE antall_dager_per_meldeperiode IS NOT NULL AND virkningsperiode_fra_og_med is not null and virkningsperiode_til_og_med is not null;