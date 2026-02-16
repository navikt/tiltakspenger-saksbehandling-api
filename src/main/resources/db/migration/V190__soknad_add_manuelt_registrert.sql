ALTER TABLE søknad ADD COLUMN IF NOT EXISTS manuelt_registrert boolean not null default false;

UPDATE søknad
SET manuelt_registrert = true
WHERE soknadstype != 'DIGITAL';

ALTER TABLE søknad
    ALTER COLUMN manuelt_registrert DROP DEFAULT;