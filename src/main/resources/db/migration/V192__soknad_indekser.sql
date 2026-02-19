CREATE INDEX ON søknad (sak_id);
CREATE INDEX ON behandling (soknad_id);
CREATE INDEX ON søknad (avbrutt) WHERE avbrutt IS NULL;
CREATE INDEX ON søknad (avbrutt) WHERE avbrutt IS NOT NULL;
CREATE INDEX ON søknadstiltak (tiltaksdeltaker_id);
CREATE INDEX ON søknad (soknadstype, opprettet) WHERE avbrutt IS NULL;