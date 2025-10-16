ALTER TABLE utbetaling
    ADD COLUMN IF NOT EXISTS satstype VARCHAR NULL;

ALTER TABLE utbetaling
    ADD CONSTRAINT valid_satstype
        CHECK (satstype IN ('DAGLIG', 'DAGLIG_INKL_HELG'));

-- Setter satstype til DAGLIG dersom vi har sent DAGLIG til utbetaling, eller utbetaling ikke er sendt ennå
-- Alle nye utbetaling etter overgang fra DAGLIG_INKL_HELG/DAG7 vil være en av disse
UPDATE utbetaling
SET satstype = CASE
    WHEN EXISTS (
        SELECT 1
        FROM jsonb_array_elements(COALESCE(utbetaling_metadata->'request'->'vedtak'->'utbetalinger', '[]'::jsonb)) AS elem
        WHERE elem->>'satstype' = 'DAGLIG'
    )
        THEN 'DAGLIG'
    ELSE 'DAGLIG_INKL_HELG'
END;

ALTER TABLE utbetaling
    ALTER COLUMN satstype SET NOT NULL;

-- Oppdaterer viewet med satstype
DROP VIEW utbetaling_full;

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

