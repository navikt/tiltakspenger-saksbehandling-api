ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS behandles_automatisk boolean NOT NULL DEFAULT false;
ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS behandlet_automatisk_status varchar NULL;

ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS brukers_meldekort_id varchar NULL;
