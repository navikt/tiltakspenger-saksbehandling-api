-- Brukes av ON CONFLICT i lagreNy for å hindre duplikate hendelser fra Kafka.
CREATE UNIQUE INDEX IF NOT EXISTS tilbakekreving_hendelse_fagsak_type_opprettet_unique
    ON tilbakekreving_hendelse (ekstern_fagsak_id, hendelse_type, opprettet);

