create table tiltaksdeltaker
(
    id         varchar primary key,
    ekstern_id varchar not null
);

create index idx_tiltaksdeltaker_ekstern_id ON tiltaksdeltaker (ekstern_id);