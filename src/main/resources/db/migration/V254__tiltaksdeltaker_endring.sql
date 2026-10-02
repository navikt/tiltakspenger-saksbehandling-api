-- Tabellen blir en generell sporbarhetstabell for endringer i tiltaksdeltakelse, uavhengig av kilde.
-- Verdien fra kilden lagres ordrett, og de utledede kolonnene dekkes av den.
ALTER TABLE tiltaksdeltaker_kafka
    RENAME TO tiltaksdeltaker_endring;

ALTER TABLE tiltaksdeltaker_endring
    RENAME COLUMN hendelse_id TO id;
ALTER TABLE tiltaksdeltaker_endring
    RENAME COLUMN deltaker_id TO ekstern_deltaker_id;
ALTER TABLE tiltaksdeltaker_endring
    RENAME COLUMN melding TO verdi;
ALTER TABLE tiltaksdeltaker_endring
    RENAME COLUMN sist_oppdatert TO opprettet;

ALTER TABLE tiltaksdeltaker_endring
    DROP COLUMN IF EXISTS deltakelse_periode,
    DROP COLUMN IF EXISTS dager_per_uke,
    DROP COLUMN IF EXISTS deltakelsesprosent,
    DROP COLUMN IF EXISTS deltakerstatus;

-- Den tolkede endringen, når en endring behandles. Skrives kun, for sporbarhet.
ALTER TABLE tiltaksdeltaker_endring
    ADD COLUMN IF NOT EXISTS endring jsonb;

ALTER INDEX IF EXISTS tiltaksdeltaker_kafka_pkey RENAME TO tiltaksdeltaker_endring_pkey;
ALTER INDEX IF EXISTS idx_tiltaksdeltaker_kafka_oppgave_id RENAME TO idx_tiltaksdeltaker_endring_oppgave_id;
ALTER INDEX IF EXISTS idx_tiltaksdeltaker_kafka_deltaker_id RENAME TO idx_tiltaksdeltaker_endring_ekstern_deltaker_id;
ALTER INDEX IF EXISTS tiltaksdeltaker_kafka_sak_id_idx RENAME TO tiltaksdeltaker_endring_sak_id_idx;

CREATE INDEX IF NOT EXISTS idx_tiltaksdeltaker_endring_tiltaksdeltaker_id ON tiltaksdeltaker_endring (tiltaksdeltaker_id);
