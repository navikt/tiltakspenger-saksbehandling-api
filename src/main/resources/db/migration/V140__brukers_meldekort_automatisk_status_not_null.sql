UPDATE meldekort_bruker
SET behandlet_automatisk_status =
        CASE
            WHEN behandles_automatisk = true
                THEN 'VENTER_BEHANDLING'
            ELSE 'SKAL_IKKE_BEHANDLES_AUTOMATISK'
            END
WHERE behandlet_automatisk_status is null;

ALTER TABLE meldekort_bruker
    ALTER COLUMN behandlet_automatisk_status SET NOT NULL;
