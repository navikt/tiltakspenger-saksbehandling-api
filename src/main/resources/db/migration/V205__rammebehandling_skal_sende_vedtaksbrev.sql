ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS skal_sende_vedtaksbrev boolean not null default true;
