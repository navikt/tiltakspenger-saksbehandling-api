CREATE TABLE klagebehandling
(
    id                    VARCHAR PRIMARY KEY,
    sak_id                VARCHAR     NOT NULL REFERENCES sak (id),
    opprettet             TIMESTAMPTZ NOT NULL,
    sist_endret           TIMESTAMPTZ NOT NULL,
    status                VARCHAR     NOT NULL,
    formkrav              jsonb       NOT NULL,
    saksbehandler         VARCHAR,
    journalpost_id        VARCHAR,
    journalpost_opprettet TIMESTAMPTZ,
    resultat              jsonb
);
