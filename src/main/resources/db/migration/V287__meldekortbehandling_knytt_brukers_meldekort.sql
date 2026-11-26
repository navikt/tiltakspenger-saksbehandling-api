-- Knytter manuelle meldekortbehandlinger fra før koblingene ble innført til meldekortene fra bruker som de behandlet.
-- En manuell behandling behandlet meldekortene på kjedene sine som var mottatt før den sist ble sendt til beslutning, og som ikke er knyttet til en tidligere behandling.
-- Åpne behandlinger knyttes til alle ukoblede meldekort på kjedene sine, slik appen gjør når de lagres.
--
-- Regelen trenger ikke å gå gjennom behandlingene i rekkefølge.
-- Et ukoblet meldekort knyttes til den første behandlingen av kjeden, sortert på opprettet, som det var mottatt før.
--
-- Avbrutte behandlinger beholder koblingene sine i appen, men koblingene teller ikke når appen finner ubehandlede meldekort.
-- En avbrutt behandling knyttes derfor til meldekortene på kjedene sine som var mottatt før den ble sendt til beslutning, eller før den ble avbrutt om tidspunktet for sendingen mangler.
-- Appen nuller sendt_til_beslutning ved avbrytelse, så i praksis gjelder avbrytelsestidspunktet.
-- Meldekort som er behandlet av en tidligere behandling av kjeden som ikke er avbrutt, knyttes ikke.
-- Det samme meldekortet kan dermed være knyttet både til en avbrutt behandling og til behandlingen som tok over.
--
-- Eksisterende koblinger beholdes, og meldekort som allerede er knyttet til en behandling som ikke er avbrutt, knyttes ikke på nytt.
-- Migreringen kan dermed kjøres flere ganger uten å endre resultatet.
WITH behandling_av_kjede AS (
    SELECT m.id,
           m.sak_id,
           m.opprettet,
           m.status,
           meldeperiode ->> 'kjedeId' AS kjede_id,
           meldeperiode -> 'brukersMeldekortIder' AS brukers_meldekort_ider,
           CASE
               WHEN m.status IN ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING') THEN 'infinity'::timestamptz
               WHEN m.status = 'AVBRUTT' THEN coalesce(m.sendt_til_beslutning, (m.avbrutt ->> 'avbruttTidspunkt')::timestamptz)
               ELSE m.sendt_til_beslutning
           END AS frist
    FROM meldekortbehandling m
    CROSS JOIN jsonb_array_elements(m.meldeperioder) AS meldeperiode
),
kandidat AS (
    SELECT * FROM behandling_av_kjede WHERE status NOT IN ('AUTOMATISK_BEHANDLET', 'AVBRUTT')
),
avbrutt AS (
    SELECT * FROM behandling_av_kjede WHERE status = 'AVBRUTT'
),
koblet AS (
    SELECT b.id AS meldekortbehandling_id,
           b.opprettet,
           b.kjede_id,
           brukers_meldekort_id
    FROM behandling_av_kjede b
    CROSS JOIN jsonb_array_elements_text(b.brukers_meldekort_ider) AS brukers_meldekort_id
    WHERE b.status <> 'AVBRUTT'
),
ny_kobling AS (
    SELECT DISTINCT ON (mb.id) mb.id AS brukers_meldekort_id,
                               k.id AS meldekortbehandling_id,
                               k.opprettet,
                               k.kjede_id
    FROM meldekort_bruker mb
    JOIN kandidat k
        ON k.sak_id = mb.sak_id
        AND k.kjede_id = mb.meldeperiode_kjede_id
        AND mb.mottatt < k.frist
    WHERE NOT EXISTS (SELECT 1 FROM koblet WHERE koblet.brukers_meldekort_id = mb.id)
    ORDER BY mb.id, k.opprettet, k.id
),
behandlet AS (
    SELECT brukers_meldekort_id, opprettet FROM koblet
    UNION ALL
    SELECT brukers_meldekort_id, opprettet FROM ny_kobling
),
ny_avbrutt_kobling AS (
    SELECT mb.id AS brukers_meldekort_id,
           a.id AS meldekortbehandling_id,
           a.kjede_id
    FROM meldekort_bruker mb
    JOIN avbrutt a
        ON a.sak_id = mb.sak_id
        AND a.kjede_id = mb.meldeperiode_kjede_id
        AND mb.mottatt < a.frist
    WHERE NOT EXISTS (
        SELECT 1
        FROM behandlet b
        WHERE b.brukers_meldekort_id = mb.id
          AND b.opprettet < a.opprettet
    )
),
alle_nye_koblinger AS (
    SELECT brukers_meldekort_id, meldekortbehandling_id, kjede_id FROM ny_kobling
    UNION ALL
    SELECT brukers_meldekort_id, meldekortbehandling_id, kjede_id FROM ny_avbrutt_kobling
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
                       FROM alle_nye_koblinger n
                       WHERE n.meldekortbehandling_id = m.id
                         AND n.kjede_id = meldeperiode ->> 'kjedeId'
                   )
            )
        )
        ORDER BY ordinalitet
    )
    FROM jsonb_array_elements(m.meldeperioder) WITH ORDINALITY AS t(meldeperiode, ordinalitet)
)
WHERE m.id IN (SELECT meldekortbehandling_id FROM alle_nye_koblinger);
