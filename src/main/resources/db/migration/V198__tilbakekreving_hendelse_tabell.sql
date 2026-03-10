create table tilbakekreving_hendelse (
    id varchar primary key,
    opprettet TIMESTAMPTZ not null,
    hendelse_type varchar not null,
    key varchar not null,
    value jsonb not null,

    ekstern_fagsak_id varchar not null,
    kravgrunnlag_referanse varchar null,
    svar jsonb null,
    sak_id varchar null REFERENCES sak (id),
    behandlet TIMESTAMPTZ null,
    behandlet_feil varchar null
);
