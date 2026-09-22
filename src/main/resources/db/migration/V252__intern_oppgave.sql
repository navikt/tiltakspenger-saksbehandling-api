CREATE TABLE intern_oppgave
(
    id             TEXT PRIMARY KEY,
    sak_id         TEXT NOT NULL REFERENCES sak (id),
    type           TEXT NOT NULL,
    nokkel         TEXT NOT NULL,
    grunnlag       JSONB NOT NULL,
    opprettet      TIMESTAMP NOT NULL,
    sist_endret    TIMESTAMP NOT NULL,
    versjon        BIGINT NOT NULL CHECK (versjon >= 0),
    saksbehandler  TEXT CHECK (saksbehandler IS NULL OR LENGTH(TRIM(saksbehandler)) > 0),
    losning        TEXT,
    behandling_id  TEXT REFERENCES behandling (id),
    lost           TIMESTAMP,
    CONSTRAINT intern_oppgave_tid CHECK (sist_endret >= opprettet),
    CONSTRAINT intern_oppgave_losning CHECK (
        (losning IS NULL AND lost IS NULL AND behandling_id IS NULL)
        OR
        (losning IS NOT NULL AND lost IS NOT NULL AND saksbehandler IS NOT NULL AND lost = sist_endret AND (
            (losning = 'FORKASTET' AND behandling_id IS NULL)
            OR (losning IN ('STANS', 'FORLENGELSE', 'OMGJORING') AND behandling_id IS NOT NULL)
        ))
    )
);

CREATE UNIQUE INDEX intern_oppgave_en_apen_per_nokkel
    ON intern_oppgave (sak_id, type, nokkel)
    WHERE lost IS NULL;

CREATE INDEX intern_oppgave_uloste
    ON intern_oppgave (opprettet, id)
    WHERE lost IS NULL;

CREATE INDEX intern_oppgave_sak
    ON intern_oppgave (sak_id, opprettet, id);
