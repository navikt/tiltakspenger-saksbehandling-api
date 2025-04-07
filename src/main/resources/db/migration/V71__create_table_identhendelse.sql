CREATE TABLE identhendelse
(
    id                 uuid PRIMARY KEY,
    gammelt_fnr        varchar                                            not null,
    nytt_fnr           varchar                                            not null,
    personidenter      jsonb                                              not null,
    sak_id             varchar                                            NOT NULL REFERENCES sak (id),
    produsert_hendelse timestamp with time zone,
    oppdatert_database timestamp with time zone,
    opprettet          timestamp with time zone default CURRENT_TIMESTAMP not null,
    sist_oppdatert     timestamp with time zone default CURRENT_TIMESTAMP not null
);

CREATE INDEX idx_identhendelse_produsert_hendelse ON identhendelse (produsert_hendelse);
CREATE INDEX idx_identhendelse_oppdatert_database ON identhendelse (oppdatert_database);