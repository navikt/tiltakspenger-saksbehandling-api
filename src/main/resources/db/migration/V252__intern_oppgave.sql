CREATE TABLE intern_oppgave
(
    id                    TEXT PRIMARY KEY,
    sak_id                TEXT NOT NULL REFERENCES sak (id),
    type                  TEXT NOT NULL,
    nøkkel                TEXT NOT NULL,
    grunnlag              JSONB NOT NULL,
    opprettet             TIMESTAMP NOT NULL,
    sist_endret           TIMESTAMP NOT NULL,
    versjon               BIGINT NOT NULL CHECK (versjon >= 0),
    saksbehandler         TEXT CHECK (saksbehandler IS NULL OR LENGTH(TRIM(saksbehandler)) > 0),
    løsning               TEXT,
    behandling_id         TEXT REFERENCES behandling (id),
    begrunnelse           TEXT,
    begrunnelse_fritekst  TEXT,
    løst                  TIMESTAMP,
    dialog                JSONB NOT NULL CHECK (jsonb_typeof(dialog) = 'array'),
    CONSTRAINT intern_oppgave_tid CHECK (sist_endret >= opprettet),
    CONSTRAINT intern_oppgave_løsning CHECK (
        (løsning IS NULL AND løst IS NULL AND behandling_id IS NULL)
        OR
        (løsning IS NOT NULL AND løst IS NOT NULL AND saksbehandler IS NOT NULL AND løst = sist_endret AND (
            (løsning = 'FORKASTET' AND behandling_id IS NULL)
            OR (løsning IN ('STANS', 'FORLENGELSE', 'OMGJORING') AND behandling_id IS NOT NULL)
        ))
    ),
    -- Begrunnelsen er enten en forhåndsdefinert årsak eller fritekst, og følger løsningen.
    CONSTRAINT intern_oppgave_begrunnelse CHECK (
        (løst IS NULL AND begrunnelse IS NULL AND begrunnelse_fritekst IS NULL)
        OR
        (løst IS NOT NULL AND (
            (begrunnelse IS NOT NULL AND begrunnelse_fritekst IS NULL)
            OR (begrunnelse IS NULL AND begrunnelse_fritekst IS NOT NULL AND LENGTH(TRIM(begrunnelse_fritekst)) > 0)
        ))
    )
);

CREATE UNIQUE INDEX intern_oppgave_en_åpen_per_nøkkel
    ON intern_oppgave (sak_id, type, nøkkel)
    WHERE løst IS NULL;

CREATE INDEX intern_oppgave_uløste
    ON intern_oppgave (opprettet, id)
    WHERE løst IS NULL;

CREATE INDEX intern_oppgave_sak
    ON intern_oppgave (sak_id, opprettet, id);
