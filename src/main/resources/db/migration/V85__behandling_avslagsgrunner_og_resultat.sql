ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS avslagsgrunner jsonb DEFAULT null;

ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS utfall varchar;

UPDATE behandling
SET utfall = CASE
    -- Finnes vedtak for behandlingen...
                 WHEN (select count(*) > 0
                       from rammevedtak
                       where behandling_id = behandling.id
                         and vedtakstype = 'Innvilgelse') then 'INNVILGELSE'
                 when (select count(*) > 0
                       from rammevedtak
                       where behandling_id = behandling.id
                         and vedtakstype = 'Stans') then 'STANS'

    -- Finnes ikke noen for behandlingen vedtak...
                 WHEN (select count(*) = 0
                       from rammevedtak
                       where behandling_id = behandling.id) and behandling.behandlingstype = 'FØRSTEGANGSBEHANDLING'
                     and behandling.status IN ('KLAR_TIL_BESLUTNING', 'UNDER_BESLUTNING', 'VEDTATT')
                     then 'INNVILGELSE'
                 WHEN (select count(*) = 0
                       from rammevedtak
                       where behandling_id = behandling.id) and behandling.behandlingstype = 'REVURDERING' then 'STANS'
    END;


