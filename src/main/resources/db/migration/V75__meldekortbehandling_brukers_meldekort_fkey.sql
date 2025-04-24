ALTER TABLE meldekortbehandling
    ADD CONSTRAINT meldekortbehandling_brukersmeldekort_id_fkey
        FOREIGN KEY (brukers_meldekort_id) REFERENCES meldekort_bruker(id);

CREATE UNIQUE INDEX idx_meldekortbehandling_automatisk_brukersmeldekort_id_unique
    ON meldekortbehandling (brukers_meldekort_id) where status = 'AUTOMATISK_BEHANDLET'
