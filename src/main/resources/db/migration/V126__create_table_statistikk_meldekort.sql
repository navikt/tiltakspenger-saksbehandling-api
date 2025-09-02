CREATE TABLE statistikk_meldekort
(
    meldeperiode_kjede_id  varchar                                            not null,
    sak_id                 varchar                                            NOT NULL,
    meldekortbehandling_id varchar                                            not null,
    bruker_id              varchar                                            not null,
    saksnummer             varchar                                            NOT NULL,
    vedtatt_tidspunkt      timestamp with time zone                           NOT NULL,
    behandlet_automatisk   boolean                                            not null,
    fra_og_med             date                                               not null,
    til_og_med             date                                               not null,
    meldekortdager         jsonb                                              not null,
    opprettet              timestamp with time zone default CURRENT_TIMESTAMP not null,
    sist_endret            timestamp with time zone default CURRENT_TIMESTAMP not null,
    primary key (meldeperiode_kjede_id, sak_id)
);