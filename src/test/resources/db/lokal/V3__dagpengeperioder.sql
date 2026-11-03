-- Kun for lokal kjøring (LokalMain), se LokalFlywayMigrate.kt.
-- Dagpengeperioder satt via dev-endepunktet, som DagpengerFakeLokalKlient svarer med for personen.
create table lokal.dagpengeperioder
(
    fnr         varchar     primary key,
    perioder    jsonb       not null,
    sist_endret timestamptz not null
);
