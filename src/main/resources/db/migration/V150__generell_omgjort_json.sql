ALTER TABLE behandling ADD COLUMN IF NOT EXISTS omgjør_rammevedtak jsonb DEFAULT null;
ALTER TABLE rammevedtak ADD COLUMN IF NOT EXISTS omgjort_av_rammevedtak jsonb DEFAULT null;
