CREATE TABLE klagehendelse (
    id varchar primary key,
    ekstern_id varchar not null unique,
    sak_id varchar,
    klage_id varchar,
    mottatt_data JSONB not null,
    opprettet TIMESTAMPTZ not null default CURRENT_TIMESTAMP,
    sist_endret TIMESTAMPTZ not null default CURRENT_TIMESTAMP,

    foreign key (klage_id) references klagebehandling(id),
    foreign key (sak_id) references sak(id)
);