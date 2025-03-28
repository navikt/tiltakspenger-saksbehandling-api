DO
$$
BEGIN
        IF
EXISTS
                (SELECT 1 from pg_roles where rolname = 'cloudsqliamuser')
        THEN
            GRANT USAGE ON SCHEMA public TO cloudsqliamuser;
            GRANT
SELECT
ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
ALTER
DEFAULT PRIVILEGES IN SCHEMA public GRANT
SELECT
ON TABLES TO cloudsqliamuser;
END IF;
END
$$;

create TABLE sak
(
    id          VARCHAR PRIMARY KEY,
    ident       VARCHAR     NOT NULL,
    saksnummer  VARCHAR     NOT NULL UNIQUE,
    sist_endret TIMESTAMPTZ NOT NULL,
    opprettet   TIMESTAMPTZ NOT NULL
);

CREATE TABLE behandling
(
    id                  VARCHAR PRIMARY KEY,
    sak_id              VARCHAR     NOT NULL REFERENCES sak (id),
    fra_og_med                 DATE        NOT NULL,
    til_og_med                 DATE        NOT NULL,
    status              VARCHAR     NOT NULL,
    saksbehandler       VARCHAR NULL,
    beslutter           VARCHAR NULL,
    iverksatt_tidspunkt TIMESTAMPTZ NULL,
    sist_endret         TIMESTAMPTZ NOT NULL,
    opprettet           TIMESTAMPTZ NOT NULL,
    vilkårssett         JSONB       NOT NULL,
    stønadsdager        JSONB       NOT NULL,
    attesteringer       JSONB NULL
);

CREATE TABLE rammevedtak
(
    id                      VARCHAR PRIMARY KEY,
    sak_id                  VARCHAR NULL REFERENCES sak (id),
    behandling_id           VARCHAR NULL REFERENCES behandling (id),
    vedtakstype             VARCHAR     NOT NULL,
    vedtaksdato             TIMESTAMPTZ NOT NULL,
    fra_og_med                     DATE        NOT NULL,
    til_og_med                     DATE        NOT NULL,
    saksbehandler           VARCHAR     NOT NULL,
    beslutter               VARCHAR     NOT NULL,
    opprettet               TIMESTAMPTZ NOT NULL,
    journalpost_id          VARCHAR NULL,
    journalføringstidspunkt TIMESTAMPTZ NULL,
    distribusjon_id         VARCHAR NULL,
    distribusjonstidspunkt  TIMESTAMPTZ NULL
);

CREATE TABLE søknad
(
    id                         VARCHAR PRIMARY KEY,
    versjon                    VARCHAR     NOT NULL,
    sak_id                     VARCHAR NULL REFERENCES sak (id),
    behandling_id              VARCHAR NULL REFERENCES behandling (id),
    ident                      VARCHAR     NOT NULL,
    fornavn                    VARCHAR     NOT NULL,
    etternavn                  VARCHAR     NOT NULL,
    journalpost_id             VARCHAR     NOT NULL,
    opprettet                  TIMESTAMPTZ NULL,
    tidsstempel_hos_oss        TIMESTAMPTZ NOT NULL,
    kvp_type                   VARCHAR     NOT NULL,
    kvp_ja                     BOOLEAN NULL,
    kvp_fom                    DATE NULL,
    kvp_tom                    DATE NULL,
    intro_type                 VARCHAR     NOT NULL,
    intro_ja                   BOOLEAN NULL,
    intro_fom                  DATE NULL,
    intro_tom                  DATE NULL,
    institusjon_type           VARCHAR     NOT NULL,
    institusjon_ja             BOOLEAN NULL,
    institusjon_fom            DATE NULL,
    institusjon_tom            DATE NULL,
    sykepenger_type            VARCHAR     NOT NULL,
    sykepenger_ja              BOOLEAN NULL,
    sykepenger_fom             DATE NULL,
    sykepenger_tom             DATE NULL,
    supplerende_alder_type     VARCHAR     NOT NULL,
    supplerende_alder_ja       BOOLEAN NULL,
    supplerende_alder_fom      DATE NULL,
    supplerende_alder_tom      DATE NULL,
    supplerende_flyktning_type VARCHAR     NOT NULL,
    supplerende_flyktning_ja   BOOLEAN NULL,
    supplerende_flyktning_fom  DATE NULL,
    supplerende_flyktning_tom  DATE NULL,
    jobbsjansen_type           VARCHAR     NOT NULL,
    jobbsjansen_ja             BOOLEAN NULL,
    jobbsjansen_fom            DATE NULL,
    jobbsjansen_tom            DATE NULL,
    gjenlevendepensjon_type    VARCHAR     NOT NULL,
    gjenlevendepensjon_ja      BOOLEAN NULL,
    gjenlevendepensjon_fom     DATE NULL,
    gjenlevendepensjon_tom     DATE NULL,
    alderspensjon_type         VARCHAR     NOT NULL,
    alderspensjon_ja           BOOLEAN NULL,
    alderspensjon_fom          DATE NULL,
    trygd_og_pensjon_type      VARCHAR     NOT NULL,
    trygd_og_pensjon_ja        BOOLEAN NULL,
    trygd_og_pensjon_fom       DATE NULL,
    trygd_og_pensjon_tom       DATE NULL,
    etterlonn_type             VARCHAR     NOT NULL,
    vedlegg                    INT         NOT NULL
);

CREATE TABLE søknad_barnetillegg
(
    id                 VARCHAR PRIMARY KEY,
    søknad_id          VARCHAR NOT NULL REFERENCES søknad (id),
    type               VARCHAR NOT NULL,
    fodselsdato        DATE NULL,
    fornavn            VARCHAR NULL,
    mellomnavn         VARCHAR NULL,
    etternavn          VARCHAR NULL,
    opphold_i_eos_type VARCHAR NOT NULL
);

CREATE TABLE søknadstiltak
(
    id             VARCHAR PRIMARY KEY,
    søknad_id      VARCHAR NOT NULL REFERENCES søknad (id),
    ekstern_id     VARCHAR NOT NULL,
    arrangørnavn   VARCHAR NULL,
    typekode       VARCHAR NOT NULL,
    typenavn       VARCHAR NOT NULL,
    deltakelse_fra_og_med DATE    NOT NULL,
    deltakelse_til_og_med DATE NULL
);

create table meldekort
(
    id                   varchar primary key,
    sak_id               varchar not null REFERENCES sak (id),
    rammevedtak_id       varchar not null REFERENCES rammevedtak (id),
    forrige_meldekort_id varchar null references meldekort(id),
    fra_og_med           date    not null,
    til_og_med           date    not null,
    meldekortdager       jsonb   not null,
    saksbehandler        varchar null,
    beslutter            varchar null,
    status               varchar not null,
    navkontor            varchar default null,
    meldeperiode_id      varchar not null,
    iverksatt_tidspunkt  timestamptz null
);

create table utbetalingsvedtak
(
    id                             varchar primary key,
    sak_id                         varchar     not null references sak (id),
    rammevedtak_id                 varchar     not null references rammevedtak (id),
    vedtakstidspunkt               timestamptz not null,
    forrige_vedtak_id              varchar null references utbetalingsvedtak(id),
    meldekort_id                   varchar     not null references meldekort (id),
    sendt_til_utbetaling_tidspunkt timestamptz null,
    journalpost_id                 varchar null,
    journalføringstidspunkt        timestamptz null,
    utbetaling_metadata            jsonb null
);

create table statistikk_utbetaling
(
    id                varchar primary key,
    sak_id            varchar not null,
    saksnummer        varchar not null,
    beløp             int     not null,
    beløp_beskrivelse varchar not null,
    årsak             varchar not null,
    posteringsdato    date    not null,
    gyldig_fra_dato   date    not null,
    gyldig_til_dato   date    not null
);

create table statistikk_stønad
(
    id                     varchar primary key,
    bruker_id              varchar null,
    sak_id                 varchar null,
    saksnummer             varchar null,
    resultat               varchar null,
    sak_dato               date null,
    gyldig_fra_dato        date null,
    gyldig_til_dato        date null,
    ytelse                 varchar null,
    søknad_id              varchar null,
    opplysning             varchar null,
    søknad_dato            date null,
    gyldig_fra_dato_søknad date null,
    gyldig_til_dato_søknad date null,
    vedtak_id              varchar null,
    type                   varchar null,
    vedtak_dato            date null,
    fra_og_med                    date null,
    til_og_med                    date null,
    oppfølging_enhet_kode  varchar null,
    oppfølging_enhet_navn  varchar null,
    beslutning_enhet_kode  varchar null,
    beslutning_enhet_navn  varchar null,
    tilhørighet_enhet_kode varchar null,
    tilhørighet_enhet_navn varchar null,
    vilkår_id              varchar null,
    vilkår_type            varchar null,
    vilkår_status          varchar null,
    lovparagraf            varchar null,
    beskrivelse            varchar null,
    gyldig_fra_dato_vilkår date null,
    gyldig_til_dato_vilkår date null,
    tiltak_id              varchar null,
    tiltak_type            varchar null,
    tiltak_beskrivelse     varchar null,
    fagsystem              varchar null,
    tiltak_dato            date null,
    gyldig_fra_dato_tiltak date null,
    gyldig_til_dato_tiltak date null,
    sist_endret            timestamptz null,
    opprettet              timestamptz null
);

create table statistikk_sak
(
    id                         serial primary key,
    sak_id                     varchar null,
    saksnummer                 varchar null,
    behandlingid               varchar null,
    relatertbehandlingid       varchar null,
    ident                      varchar null,
    mottatt_tidspunkt          timestamptz null,
    registrerttidspunkt        timestamptz null,
    ferdigbehandlettidspunkt   timestamptz null,
    vedtaktidspunkt            timestamptz null,
    utbetalttidspunkt          timestamptz null,
    endrettidspunkt            timestamptz null,
    søknadsformat              varchar null,
    forventetoppstarttidspunkt timestamptz null,
    teknisktidspunkt           timestamptz null,
    sakytelse                  varchar null,
    sakutland                  boolean null,
    behandlingtype             varchar null,
    behandlingstatus           varchar null,
    behandlingresultat         varchar null,
    resultatbegrunnelse        varchar null,
    behandlingmetode           varchar null,
    opprettetav                varchar null,
    saksbehandler              varchar null,
    ansvarligbeslutter         varchar null,
    ansvarligenhet             varchar null,
    tilbakekrevingsbeløp       decimal null,
    funksjonellperiode_fra_og_med      date null,
    funksjonellperiode_til_og_med      date null,
    hendelse                   varchar null,
    avsender                   varchar null,
    versjon                    varchar null
);

create table statistikk_sak_vilkår
(
    id                serial primary key,
    statistikk_sak_id int not null references statistikk_sak (id),
    vilkår            varchar null,
    beskrivelse       varchar null,
    resultat          varchar null
);
