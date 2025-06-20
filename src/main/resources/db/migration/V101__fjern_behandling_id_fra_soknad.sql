-- Trello 1426 Fjerner behandling_id fra søknad da databasepekeren har blitt snudd slik at behandling nå har soknad_id
alter table søknad
    drop column if exists behandling_id;
