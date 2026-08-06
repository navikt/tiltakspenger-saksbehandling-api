-- Gjør meldekortene fra bruker som en meldeperiodebehandling behandler eksplisitte, også for manuelle behandlinger.
-- Feltet brukersMeldekortId (én nullable id) erstattes av brukersMeldekortIder (en liste som alltid er satt).
-- Manuelle behandlinger får foreløpig en tom liste; de knyttes til meldekortene sine i en senere endring.

UPDATE meldekortbehandling
SET meldeperioder = (
    SELECT jsonb_agg(
        (meldeperiode - 'brukersMeldekortId') || jsonb_build_object(
            'brukersMeldekortIder',
            CASE
                WHEN meldeperiode ->> 'brukersMeldekortId' IS NULL THEN '[]'::jsonb
                ELSE jsonb_build_array(meldeperiode ->> 'brukersMeldekortId')
            END
        )
        ORDER BY ordinalitet
    )
    FROM jsonb_array_elements(meldeperioder) WITH ORDINALITY AS t(meldeperiode, ordinalitet)
);

-- Indeksen som håndhever at ett meldekort fra bruker kun kan ha én automatisk behandling må peke på det nye feltet.
DROP INDEX idx_meldekortbehandling_auto_brukersmeldekort_jsonb_unique;

CREATE UNIQUE INDEX idx_meldekortbehandling_auto_brukersmeldekort_jsonb_unique
    ON meldekortbehandling ((meldeperioder -> 0 -> 'brukersMeldekortIder' ->> 0))
    WHERE status = 'AUTOMATISK_BEHANDLET';
