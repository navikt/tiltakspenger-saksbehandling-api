-- Peker på meldekortbehandlingen som behandlet meldekortet fra bruker.
-- Appen setter kolonnen når behandlingen iverksettes eller avbrytes.
-- Åpne behandlinger setter den ikke, så meldekortene uten verdi er de ubehandlede.
ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS behandlet_av varchar NULL REFERENCES meldekortbehandling (id);

-- Fyller ut kolonnen fra koblingene på de iverksatte og avbrutte behandlingene, inkludert koblingene fra V287.
-- Et meldekort er knyttet til høyst én slik behandling, men ved flere velges den første, sortert på opprettet, slik at resultatet er entydig.
-- Eksisterende verdier beholdes, så migreringen kan kjøres flere ganger uten å endre resultatet.
UPDATE meldekort_bruker mb
SET behandlet_av = behandlet.meldekortbehandling_id
FROM (
    SELECT DISTINCT ON (brukers_meldekort_id.id)
           brukers_meldekort_id.id AS brukers_meldekort_id,
           m.id AS meldekortbehandling_id
    FROM meldekortbehandling m
    CROSS JOIN jsonb_array_elements(m.meldeperioder) AS meldeperiode
    CROSS JOIN jsonb_array_elements_text(meldeperiode -> 'brukersMeldekortIder') AS brukers_meldekort_id(id)
    WHERE m.status IN ('GODKJENT', 'AUTOMATISK_BEHANDLET', 'AVBRUTT')
    ORDER BY brukers_meldekort_id.id, m.opprettet, m.id
) AS behandlet
WHERE mb.id = behandlet.brukers_meldekort_id
  AND mb.behandlet_av IS NULL;
