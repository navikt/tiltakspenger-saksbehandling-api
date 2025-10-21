DROP VIEW utbetaling_full;

ALTER TABLE utbetaling
    DROP COLUMN kan_utbetale_helg_på_fredag;

-- Oppdaterer viewet
CREATE VIEW utbetaling_full as
select u.*,
    s.saksnummer,
    s.fnr,
    r.behandling_id,
    m.meldekort_id,
    COALESCE(b.saksbehandler, mb.saksbehandler)   as saksbehandler,
    COALESCE(b.beslutter, mb.beslutter)           as beslutter,
    COALESCE(b.beregning, mb.beregninger)         as beregning,
    COALESCE(b.navkontor, mb.navkontor)           as navkontor,
    COALESCE(b.navkontor_navn, mb.navkontor_navn) as navkontor_navn
from utbetaling u
join sak s on s.id = u.sak_id
left join rammevedtak r on u.rammevedtak_id = r.id
left join meldekortvedtak m on u.meldekortvedtak_id = m.id
left join meldekortbehandling mb on mb.id = m.meldekort_id
left join behandling b on b.id = r.behandling_id;
