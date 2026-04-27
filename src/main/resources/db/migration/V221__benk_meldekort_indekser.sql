-- Indekser som brukes av spørringen ÅPNE_MELDEKORT_TIL_BEHANDLING i BenkOversiktPostgresRepo.

-- Partielt indeks: kun rader som faktisk er kandidater for benken (ikke ferdigbehandlet automatisk).
-- Når et meldekort blir BEHANDLET forsvinner det fra indekset, slik at indekset holder seg lite.
-- Brukes av DISTINCT ON (sak_id, meldeperiode_kjede_id) ORDER BY ... mottatt DESC.
CREATE INDEX idx_meldekort_bruker_apent_kjede_mottatt
    ON meldekort_bruker (sak_id, meldeperiode_kjede_id, mottatt DESC)
    WHERE behandlet_automatisk_status != 'BEHANDLET' AND behandles_automatisk = false;

-- Brukes av NOT EXISTS-sjekken som filtrerer bort meldekort der det allerede finnes en
-- meldekortbehandling som er nyere eller samtidig med innsendingen.
CREATE INDEX idx_meldekortbehandling_kjede_sist_endret
    ON meldekortbehandling (sak_id, meldeperiode_kjede_id, sist_endret);

