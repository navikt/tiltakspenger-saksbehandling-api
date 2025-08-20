CREATE TABLE utbetaling
(
    id                             varchar primary key,
    sak_id                         varchar     not null references sak (id),
    rammevedtak_id                 varchar     null references rammevedtak (id),
    meldekortvedtak_id             varchar     null references meldekortvedtak (id),
    forrige_utbetaling_id          varchar     null references utbetaling (id),
    sendt_til_utbetaling_tidspunkt timestamptz null,
    utbetaling_metadata            jsonb       null,
    status                         varchar     null,
    status_metadata                jsonb       null,

    CONSTRAINT rammevedtak_eller_meldekortvedtak CHECK (
        (rammevedtak_id IS NOT NULL AND meldekortvedtak_id IS NULL) OR
        (rammevedtak_id IS NULL AND meldekortvedtak_id IS NOT NULL)
        )
)
