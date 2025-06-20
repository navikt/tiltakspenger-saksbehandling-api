UPDATE behandling SET antall_dager_per_meldeperiode_json = null where 1=1;

UPDATE behandling
SET antall_dager_per_meldeperiode_json =
        jsonb_build_array(
                jsonb_build_object(
                        'antallDagerPerMeldeperiode', antall_dager_per_meldeperiode::int,
                        'periode', jsonb_build_object(
                                'fraOgMed', virkningsperiode_fra_og_med,
                                'tilOgMed', virkningsperiode_til_og_med
                                   )
                )
        )
WHERE antall_dager_per_meldeperiode IS NOT NULL AND virkningsperiode_fra_og_med is not null and virkningsperiode_til_og_med is not null;

ALTER TABLE behandling
    DROP COLUMN antall_dager_per_meldeperiode;

ALTER TABLE behandling
    RENAME COLUMN antall_dager_per_meldeperiode_json TO antall_dager_per_meldeperiode;