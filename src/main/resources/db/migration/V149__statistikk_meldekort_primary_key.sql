alter table statistikk_meldekort
    drop constraint statistikk_meldekort_pkey,
    add constraint statistikk_meldekort_pkey primary key (meldekortbehandling_id)