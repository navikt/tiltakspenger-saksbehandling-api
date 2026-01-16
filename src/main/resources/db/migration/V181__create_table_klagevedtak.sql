CREATE TABLE klagevedtak
(
    id                      varchar primary key,
    sak_id                  varchar     not null references sak (id),
    klagebehandling_id      varchar     not null references klagebehandling (id),
    opprettet               timestamptz not null,
    journalpost_id          varchar     null,
    journalføringstidspunkt timestamptz null,
    distribusjon_id         varchar     null,
    distribusjonstidspunkt  timestamptz null,
    sendt_til_datadeling    timestamptz null,
    vedtaksdato             date        null,
    brev_json               jsonb       null
);