CREATE TABLE personhendelse
(
    id                   uuid PRIMARY KEY,
    fnr                  varchar                                            not null,
    hendelse_id          varchar                                            not null,
    opplysningstype      varchar                                            not null,
    personhendelse_type  jsonb                                              not null,
    sak_id               varchar                                            NOT NULL REFERENCES sak (id),
    oppgave_id           varchar,
    oppgave_sist_sjekket timestamp with time zone,
    opprettet            timestamp with time zone default CURRENT_TIMESTAMP not null,
    sist_oppdatert       timestamp with time zone default CURRENT_TIMESTAMP not null
);

CREATE INDEX idx_personhendelse_fnr ON personhendelse (fnr);
CREATE INDEX idx_personhendelse_oppgave_id ON personhendelse (oppgave_id);