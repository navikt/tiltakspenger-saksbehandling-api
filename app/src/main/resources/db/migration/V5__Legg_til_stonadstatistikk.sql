create table statistikk_stønad
(
    id                      varchar                  primary key,
    bruker_id               varchar                  null,
    sak_id                  varchar                  null,
    saksnummer              varchar                  null,
    resultat                varchar                  null,
    sak_dato                date                     null,
    gyldig_fra_dato         date                     null,
    gyldig_til_dato         date                     null,
    ytelse                  varchar                  null,
    søknad_id               varchar                  null,
    opplysning              varchar                  null,
    søknad_dato             date                     null,
    gyldig_fra_dato_søknad  date                     null,
    gyldig_til_dato_søknad  date                     null,
    vedtak_id               varchar                  null,
    type                    varchar                  null,
    vedtak_dato             date                     null,
    fom                     date                     null,
    tom                     date                     null,
    oppfølging_enhet_kode   varchar                  null,
    oppfølging_enhet_navn   varchar                  null,
    beslutning_enhet_kode   varchar                  null,
    beslutning_enhet_navn   varchar                  null,
    tilhørighet_enhet_kode  varchar                  null,
    tilhørighet_enhet_navn  varchar                  null,
    vilkår_id               varchar                  null,
    vilkår_type             varchar                  null,
    vilkår_status           varchar                  null,
    lovparagraf             varchar                  null,
    beskrivelse             varchar                  null,
    gyldig_fra_dato_vilkår  date                     null,
    gyldig_til_dato_vilkår  date                     null,
    tiltak_id               varchar                  null,
    tiltak_type             varchar                  null,
    tiltak_beskrivelse      varchar                  null,
    fagsystem               varchar                  null,
    tiltak_dato             date                     null,
    gyldig_fra_dato_tiltak  date                     null,
    gyldig_til_dato_tiltak  date                     null,
    sist_endret             timestamp with time zone null,
    opprettet               timestamp with time zone null
);
