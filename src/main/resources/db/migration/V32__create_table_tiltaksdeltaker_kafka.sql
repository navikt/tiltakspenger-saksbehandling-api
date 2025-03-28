CREATE TABLE tiltaksdeltaker_kafka
(
    id                    varchar PRIMARY KEY,
    deltakelse_fra_og_med date,
    deltakelse_til_og_med date,
    dager_per_uke         double precision,
    deltakelsesprosent    double precision,
    deltakerstatus        varchar     NOT NULL,
    sak_id                varchar     NOT NULL REFERENCES sak (id),
    oppgave_id            varchar,
    sist_oppdatert        TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tiltaksdeltaker_kafka_oppgave_id ON tiltaksdeltaker_kafka (oppgave_id);
CREATE INDEX idx_soknadstiltak_ekstern_id ON søknadstiltak (ekstern_id);