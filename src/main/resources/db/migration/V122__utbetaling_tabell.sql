CREATE TABLE utbetaling
(
    id                             varchar primary key,
    sak_id                         varchar     not null references sak (id),
    rammevedtak_id                 varchar     null unique references rammevedtak (id),
    meldekortvedtak_id             varchar     null unique references meldekortvedtak (id),
    forrige_utbetaling_id          varchar     null unique references utbetaling(id),
    sendt_til_utbetaling_tidspunkt timestamptz null,
    utbetaling_metadata            jsonb       null,
    status                         varchar     null,
    status_metadata                jsonb       null,

    CONSTRAINT rammevedtak_eller_meldekortvedtak CHECK (
        (rammevedtak_id IS NOT NULL AND meldekortvedtak_id IS NULL) OR
        (rammevedtak_id IS NULL AND meldekortvedtak_id IS NOT NULL)
        )
);

ALTER TABLE meldekortvedtak
    ADD COLUMN IF NOT EXISTS utbetaling_id VARCHAR NULL references utbetaling (id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE rammevedtak
    ADD COLUMN IF NOT EXISTS utbetaling_id VARCHAR NULL references utbetaling (id) DEFERRABLE INITIALLY DEFERRED;

CREATE VIEW utbetaling_full as (
    select
        u.*,
        s.saksnummer,
        s.fnr,
        r.behandling_id,
        m.meldekort_id,
        COALESCE(r.opprettet, m.opprettet) as opprettet,
        COALESCE(b.saksbehandler, mb.saksbehandler) as saksbehandler,
        COALESCE(b.beslutter, mb.beslutter) as beslutter,
        COALESCE(b.beregning, mb.beregninger) as beregning,
        COALESCE(b.navkontor, mb.navkontor) as navkontor,
        COALESCE(b.navkontor_navn, mb.navkontor_navn) as navkontor_navn
    from utbetaling u
    join sak s on s.id = u.sak_id
    left join rammevedtak r on u.rammevedtak_id = r.id
    left join meldekortvedtak m on u.meldekortvedtak_id = m.id
    left join meldekortbehandling mb on mb.id = m.meldekort_id
    left join behandling b on b.id = r.behandling_id
);
