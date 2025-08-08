CREATE INDEX IF NOT EXISTS idx_behandling_ersattpavent_last
    ON behandling ((ventestatus -> 'ventestatusHendelser' -> -1 ->> 'erSattPåVent'));
