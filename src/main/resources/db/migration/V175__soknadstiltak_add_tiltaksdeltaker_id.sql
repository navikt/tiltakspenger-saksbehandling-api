ALTER TABLE søknadstiltak ADD COLUMN IF NOT EXISTS tiltaksdeltaker_id VARCHAR references tiltaksdeltaker(id);
