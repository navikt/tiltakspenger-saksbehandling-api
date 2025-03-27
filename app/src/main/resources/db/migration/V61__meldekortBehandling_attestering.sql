ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS attesteringer JSONB DEFAULT '{"attesteringer": []}'::jsonb;


--oppdaterer begrunnelse i hver attestering til null, dersom det er en tom string.
UPDATE behandling
SET attesteringer = jsonb_set(
        attesteringer,
        '{attesteringer}',
        COALESCE(
                (SELECT jsonb_agg(
                                CASE
                                    WHEN att ->> 'begrunnelse' = ''
                                        THEN jsonb_set(att, '{begrunnelse}', 'null'::jsonb)
                                    ELSE att
                                    END
                        )
                 FROM jsonb_array_elements(attesteringer -> 'attesteringer') att),
                '[]'::jsonb
        )
                    )
WHERE attesteringer -> 'attesteringer' IS NOT NULL;