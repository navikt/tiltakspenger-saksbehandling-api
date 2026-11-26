-- Knytter manuelle meldekortbehandlinger fra før koblingene ble innført til meldekortene fra bruker som de behandlet.
-- En manuell behandling behandlet meldekortene på kjedene sine som var mottatt før den sist ble sendt til beslutning, og som ikke er knyttet til en tidligere behandling.
-- Åpne behandlinger knyttes til alle ukoblede meldekort på kjedene sine, slik appen gjør når de lagres.
-- Avbrutte behandlinger knyttes ikke, og koblingene deres teller ikke, slik som i appen.
--
-- Regelen trenger ikke å gå gjennom behandlingene i rekkefølge.
-- Et ukoblet meldekort knyttes til den første behandlingen av kjeden, sortert på opprettet, som det var mottatt før.
-- Eksisterende koblinger beholdes, og meldekort som allerede er knyttet til en behandling som ikke er avbrutt, knyttes ikke på nytt.
-- Migreringen kan dermed kjøres flere ganger uten å endre resultatet.
WITH kandidat AS (
    SELECT m.id,
           m.sak_id,
           m.opprettet,
           meldeperiode ->> 'kjedeId' AS kjede_id,
           CASE
               WHEN m.status IN ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING') THEN 'infinity'::timestamptz
               ELSE m.sendt_til_beslutning
           END AS frist
    FROM meldekortbehandling m
    CROSS JOIN jsonb_array_elements(m.meldeperioder) AS meldeperiode
    WHERE m.status NOT IN ('AUTOMATISK_BEHANDLET', 'AVBRUTT')
),
koblet AS (
    SELECT DISTINCT brukers_meldekort_id
    FROM meldekortbehandling m
    CROSS JOIN jsonb_array_elements(m.meldeperioder) AS meldeperiode
    CROSS JOIN jsonb_array_elements_text(meldeperiode -> 'brukersMeldekortIder') AS brukers_meldekort_id
    WHERE m.status <> 'AVBRUTT'
),
ny_kobling AS (
    SELECT DISTINCT ON (mb.id) mb.id AS brukers_meldekort_id,
                               k.id AS meldekortbehandling_id,
                               k.kjede_id
    FROM meldekort_bruker mb
    JOIN kandidat k
        ON k.sak_id = mb.sak_id
        AND k.kjede_id = mb.meldeperiode_kjede_id
        AND mb.mottatt < k.frist
    WHERE NOT EXISTS (SELECT 1 FROM koblet WHERE koblet.brukers_meldekort_id = mb.id)
    ORDER BY mb.id, k.opprettet, k.id
)
UPDATE meldekortbehandling m
SET meldeperioder = (
    SELECT jsonb_agg(
        jsonb_set(
            meldeperiode,
            '{brukersMeldekortIder}',
            (
                SELECT coalesce(jsonb_agg(mb.id ORDER BY mb.mottatt, mb.id), '[]'::jsonb)
                FROM meldekort_bruker mb
                WHERE mb.id IN (SELECT jsonb_array_elements_text(meldeperiode -> 'brukersMeldekortIder'))
                   OR mb.id IN (
                       SELECT n.brukers_meldekort_id
                       FROM ny_kobling n
                       WHERE n.meldekortbehandling_id = m.id
                         AND n.kjede_id = meldeperiode ->> 'kjedeId'
                   )
            )
        )
        ORDER BY ordinalitet
    )
    FROM jsonb_array_elements(m.meldeperioder) WITH ORDINALITY AS t(meldeperiode, ordinalitet)
)
WHERE m.id IN (SELECT meldekortbehandling_id FROM ny_kobling);
