-- Vi støtter nå lagring av tilbakekrevingshendelser av type Ukjent som ikke har en ekstern_fagsak_id.
ALTER TABLE tilbakekreving_hendelse
    ALTER COLUMN ekstern_fagsak_id DROP NOT NULL;

