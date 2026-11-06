-- Tiltakshistorikken som hentes når en endret tiltaksdeltakelse behandles.
-- Et innslag lagres når verdien er endret siden forrige innslag for deltakeren, eller når endringen førte til en behandling eller en oppgave.
-- DDL-en er idempotent, slik at TiltakshistorikkMigreringAggregatTest kan kjøre hele fila på nytt mot historiske data.
CREATE TABLE IF NOT EXISTS tiltakshistorikk
(
    id                  varchar PRIMARY KEY,
    sak_id              varchar     NOT NULL REFERENCES sak (id),
    tiltaksdeltaker_id  varchar     NOT NULL REFERENCES tiltaksdeltaker (id),
    ekstern_deltaker_id varchar     NOT NULL,
    innhentet           timestamptz NOT NULL,
    verdi               jsonb       NOT NULL,
    endring             jsonb,
    behandling_id       varchar REFERENCES behandling (id),
    oppgave_id          varchar
);

CREATE INDEX IF NOT EXISTS idx_tiltakshistorikk_sak_id ON tiltakshistorikk (sak_id);
CREATE INDEX IF NOT EXISTS idx_tiltakshistorikk_tiltaksdeltaker_id_innhentet ON tiltakshistorikk (tiltaksdeltaker_id, innhentet);
CREATE INDEX IF NOT EXISTS idx_tiltakshistorikk_behandling_id ON tiltakshistorikk (behandling_id);

-- Flytter nå-tilstandene jobben har lagret i tiltaksdeltaker_endring.
-- Rader uten verdi, der deltakelsen manglet i tiltakshistorikken eller ikke kunne leses, har ingen innslag å flytte og slettes.
-- Id-en beholder ULID-delen, med prefikset til den nye typen.
INSERT INTO tiltakshistorikk (id,
                              sak_id,
                              tiltaksdeltaker_id,
                              ekstern_deltaker_id,
                              innhentet,
                              verdi,
                              endring,
                              behandling_id,
                              oppgave_id)
SELECT 'tiltakshistorikk_' || split_part(id, '_', 2),
       sak_id,
       tiltaksdeltaker_id,
       ekstern_deltaker_id,
       COALESCE(behandlet_tidspunkt, opprettet),
       verdi::jsonb,
       endring,
       behandling_id,
       oppgave_id
FROM tiltaksdeltaker_endring
WHERE kilde = 'Tiltakshistorikk'
  AND verdi IS NOT NULL
ON CONFLICT (id) DO NOTHING;

DELETE
FROM tiltaksdeltaker_endring
WHERE kilde = 'Tiltakshistorikk';
