CREATE TABLE ekstern_oppgave
(
    oppgave_id TEXT PRIMARY KEY,
    sak_id     TEXT NOT NULL REFERENCES sak (id),
    opprettet  TIMESTAMP NOT NULL,
    grunnlag   JSONB NOT NULL
);

CREATE INDEX ekstern_oppgave_sak_opprettet_idx
    ON ekstern_oppgave (sak_id, opprettet, oppgave_id);
