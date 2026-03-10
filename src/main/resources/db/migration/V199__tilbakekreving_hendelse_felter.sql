CREATE UNIQUE INDEX IF NOT EXISTS tilbakekreving_hendelse_kravgrunnlag_ref_unique ON tilbakekreving_hendelse (kravgrunnlag_referanse);

ALTER TABLE tilbakekreving_hendelse
    ADD COLUMN IF NOT EXISTS url VARCHAR NULL,
    ADD COLUMN IF NOT EXISTS behandlingsstatus VARCHAR NULL;
