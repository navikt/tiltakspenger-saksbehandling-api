-- Legger til manglende felter for omgjøring av vedtak. Beholder virkningsperiode som omgjøringsperiode/vedtaksperiode.
-- Tanken er at denne både kan være en enkelt periode for å støtte krymping/utviding basert på endret tiltaksdeltagelsesperiode, og senere også periodiseres, slik at man støtter delvis innvilgelse med hull.
-- Vil kun brukes av omgjøring til å starte med.
ALTER TABLE behandling ADD COLUMN IF NOT EXISTS innvilgelsesperiode jsonb;
ALTER TABLE behandling ADD COLUMN IF NOT EXISTS omgjør_rammevedtak_id varchar references rammevedtak(id);
ALTER TABLE rammevedtak ADD COLUMN IF NOT EXISTS omgjort_av_rammevedtak_id varchar references rammevedtak(id);