-- Kun for lokal kjøring (LokalMain), se LokalFlywayMigrate.kt.
-- Tiltaksdeltakelser satt via dev-endepunktet, som TiltaksdeltakelseFakeLokalKlient leverer i stedet for deltakelsen utledet fra søknaden.
create table lokal.tiltaksdeltakelse
(
    ekstern_id  varchar     primary key,
    fnr         varchar     not null,
    deltakelse  jsonb       not null,
    sist_endret timestamptz not null
);

create index tiltaksdeltakelse_fnr_idx on lokal.tiltaksdeltakelse (fnr);
