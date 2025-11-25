alter table statistikk_sak
    add column if not exists relatertfagsystem varchar not null default 'TPSAK';

alter table statistikk_sak
    add column if not exists sakutland varchar not null default 'NASJONAL';

alter table statistikk_sak
    add column if not exists ansvarligenhet varchar not null default '0387';
