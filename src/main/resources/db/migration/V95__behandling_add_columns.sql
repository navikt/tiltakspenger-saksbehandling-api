ALTER TABLE behandling ADD COLUMN IF NOT EXISTS automatisk_saksbehandlet boolean not null default false;

ALTER TABLE behandling ADD COLUMN IF NOT EXISTS manuelt_behandles_grunner jsonb;