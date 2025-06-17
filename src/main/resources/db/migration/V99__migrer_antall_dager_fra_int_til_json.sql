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

ALTER TABLE behandling 
DROP COLUMN antall_dager_per_meldeperiode;

ALTER TABLE behandling 
RENAME COLUMN antall_dager_per_meldeperiode_json TO antall_dager_per_meldeperiode;