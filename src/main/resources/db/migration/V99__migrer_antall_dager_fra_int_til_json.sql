ALTER TABLE behandling
    DROP COLUMN antall_dager_per_meldeperiode;

ALTER TABLE behandling
    RENAME COLUMN antall_dager_per_meldeperiode_json TO antall_dager_per_meldeperiode;