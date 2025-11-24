alter table statistikk_stonad
    drop column virkningsperiode_fra_og_med;

alter table statistikk_stonad
    drop column virkningsperiode_til_og_med;

alter table statistikk_stonad
    rename column fra_og_med to vedtaksperiode_fra_og_med;

alter table statistikk_stonad
    rename column til_og_med to vedtaksperiode_til_og_med;
