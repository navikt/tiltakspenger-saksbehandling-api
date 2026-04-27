-- Trenger ikke markere meldekortet for automatisk behandling når det allerede er behandlet
UPDATE meldekort_bruker SET behandles_automatisk = false WHERE behandlet_automatisk_status = 'BEHANDLET'