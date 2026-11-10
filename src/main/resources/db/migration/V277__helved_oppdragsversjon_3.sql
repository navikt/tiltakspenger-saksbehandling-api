-- Hvilken versjon av Hel Veds API utbetalingene i saken sendes med: 2 er REST (Utsjekk), 3 er Kafka.
-- En sak bytter bare versjon gjennom migreringen hos Hel Ved, så alle utbetalinger i en sak går samme vei.
ALTER TABLE sak
    ADD COLUMN oppdragsversjon SMALLINT NOT NULL DEFAULT 2 CHECK (oppdragsversjon IN (2, 3));

-- utbetaling_full lages på nytt for å ta med sak.oppdragsversjon, som UtbetalingPostgresRepo leser.
-- CREATE OR REPLACE VIEW kan bare legge til kolonner sist, så viewet må slettes først.
DROP VIEW utbetaling_full;

CREATE VIEW utbetaling_full as
select u.*,
    s.saksnummer,
    s.fnr,
    s.oppdragsversjon,
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

CREATE INDEX utbetaling_uuid_part_idx ON utbetaling (right(id, 15));

-- Statusmeldinger om utbetalinger, lagret rått fra Kafka og behandlet av en jobb.
-- Nøkkel og innhold kan være null; jobben tolker meldingen og kobler den til sak og utbetaling.
-- Kafka-metadata lagres som jsonb og brukes til deduplisering og offset-rekkefølge.
-- Nye metadatafelt krever ikke migrering.
-- Den unike indeksen på topic, partisjon og offset hindrer at en melding som leses på nytt, lagres to ganger.
CREATE TABLE utbetalingskvittering (
    id VARCHAR PRIMARY KEY,
    mottatt TIMESTAMPTZ NOT NULL,
    key VARCHAR,
    value TEXT,
    behandlet TIMESTAMPTZ,
    behandlet_feil VARCHAR,
    kafka JSONB NOT NULL,
    sak_id VARCHAR REFERENCES sak(id),
    utbetaling_id VARCHAR REFERENCES utbetaling(id),
    forsokshistorikk JSONB,
    forbigaaende BOOLEAN
);

CREATE UNIQUE INDEX utbetalingskvittering_kafka_unik ON utbetalingskvittering (
    (kafka ->> 'topic'),
    ((kafka ->> 'partisjon')::int),
    ((kafka ->> 'offset')::bigint)
);

CREATE INDEX utbetalingskvittering_ubehandlet_idx ON utbetalingskvittering (id) WHERE behandlet IS NULL;

CREATE INDEX utbetalingskvittering_ubehandlet_topic_partisjon_key_offset_idx
    ON utbetalingskvittering (
        (kafka ->> 'topic'),
        ((kafka ->> 'partisjon')::int),
        key,
        ((kafka ->> 'offset')::bigint)
    )
    WHERE behandlet IS NULL
      AND key IS NOT NULL;
