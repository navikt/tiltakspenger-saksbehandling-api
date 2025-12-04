-- Ny kolonne for å dokumentere manuelle endringer
ALTER TABLE søknadstiltak ADD COLUMN IF NOT EXISTS merknad VARCHAR default null;
