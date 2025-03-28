ALTER TABLE rammevedtak
    DROP COLUMN IF EXISTS vedtaksdato;
ALTER TABLE rammevedtak
    ADD COLUMN vedtaksdato DATE NULL;
ALTER TABLE rammevedtak
    ADD COLUMN IF NOT EXISTS brev_json JSONB NULL;
ALTER TABLE utbetalingsvedtak
    RENAME COLUMN vedtakstidspunkt TO opprettet