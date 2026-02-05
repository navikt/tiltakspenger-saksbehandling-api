ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS behandlet_automatisk_metadata JSONB;

UPDATE meldekort_bruker
SET behandlet_automatisk_metadata = jsonb_build_object(
        'forrigeForsøk', now()::TIMESTAMP,
        'antallForsøk', 1,
        'nesteForsøk', now()::TIMESTAMP
                                    )
WHERE behandlet_automatisk_metadata IS NULL;
