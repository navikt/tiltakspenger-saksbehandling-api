-- Knytter manuelle meldekortbehandlinger fra før koblingene ble innført til meldekortene fra bruker som de behandlet.
-- En manuell behandling behandlet meldekortene på kjedene sine som var mottatt før den sist ble sendt til beslutning, og som ikke er knyttet til en tidligere behandling.
-- Åpne behandlinger knyttes til alle ukoblede meldekort på kjedene sine, slik appen gjør når de lagres.
--
-- Avbrutte behandlinger behandler også meldekortene de er knyttet til, slik at de ikke tas med i neste behandling.
-- En avbrutt behandling knyttes til meldekortene på kjedene sine som var mottatt før den ble sendt til beslutning, eller før den ble avbrutt om tidspunktet for sendingen mangler.
-- Appen nuller sendt_til_beslutning ved avbrytelse, så i praksis gjelder avbrytelsestidspunktet.
-- Behandlinger som ble avbrutt av en rammeiverksettelse, ble lagret uten avbrytelsen.
-- For dem brukes sist_endret, som ble satt til avbrytelsestidspunktet og ikke endres etterpå.
--
-- Et ukoblet meldekort knyttes til den første manuelle eller avbrutte behandlingen av kjeden, sortert på frist, som det var mottatt før.
-- En kjede kan bare ha én åpen behandling om gangen, så fristene følger rekkefølgen behandlingene av kjeden ble avsluttet i, med en åpen behandling sist.
-- Opprettet følger ikke den rekkefølgen, siden en eldre åpen behandling kan utvides med en kjede som en nyere behandling allerede har behandlet.
-- Automatiske behandlinger er allerede knyttet til meldekortet de behandlet, og knyttes ikke til flere.
--
-- Eksisterende koblinger beholdes, og meldekort som allerede er knyttet til en behandling, knyttes ikke på nytt.
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
               WHEN m.status = 'AVBRUTT' THEN coalesce(m.sendt_til_beslutning, (m.avbrutt ->> 'avbruttTidspunkt')::timestamptz, m.sist_endret)
               ELSE m.sendt_til_beslutning
           END AS frist
    FROM meldekortbehandling m
    CROSS JOIN jsonb_array_elements(m.meldeperioder) AS meldeperiode
),
koblet AS (
    SELECT brukers_meldekort_id
    FROM behandling_av_kjede b
    CROSS JOIN jsonb_array_elements_text(b.brukers_meldekort_ider) AS brukers_meldekort_id
),
ny_kobling AS (
    SELECT DISTINCT ON (mb.id) mb.id AS brukers_meldekort_id,
                               b.id AS meldekortbehandling_id,
                               b.kjede_id
    FROM meldekort_bruker mb
    JOIN behandling_av_kjede b
        ON b.sak_id = mb.sak_id
        AND b.kjede_id = mb.meldeperiode_kjede_id
        AND mb.mottatt < b.frist
    WHERE b.status <> 'AUTOMATISK_BEHANDLET'
      AND NOT EXISTS (SELECT 1 FROM koblet WHERE koblet.brukers_meldekort_id = mb.id)
    ORDER BY mb.id, b.frist, b.opprettet, b.id
),
-- Bygger de nye listene med joins framfor korrelerte underspørringer per meldeperiode.
-- Med `OR` mellom to korrelerte `IN` skannet Postgres hele meldekort_bruker for hver meldeperiode, og migreringen tok 7,5 minutter mot 35k manuelle behandlinger.
-- Listene for meldeperiodene uten nye koblinger bygges på samme måte, slik at alle meldeperiodene i en oppdatert behandling sorteres likt.
kobling AS (
    SELECT b.id AS meldekortbehandling_id,
           b.kjede_id,
           k.brukers_meldekort_id
    FROM behandling_av_kjede b
    CROSS JOIN jsonb_array_elements_text(b.brukers_meldekort_ider) AS k(brukers_meldekort_id)
    WHERE b.id IN (SELECT meldekortbehandling_id FROM ny_kobling)
    UNION
    SELECT meldekortbehandling_id, kjede_id, brukers_meldekort_id
    FROM ny_kobling
),
ny_liste AS (
    SELECT k.meldekortbehandling_id,
           k.kjede_id,
           jsonb_agg(mb.id ORDER BY mb.mottatt, mb.id) AS brukers_meldekort_ider
    FROM kobling k
    JOIN meldekort_bruker mb ON mb.id = k.brukers_meldekort_id
    GROUP BY k.meldekortbehandling_id, k.kjede_id
),
nye_meldeperioder AS (
    SELECT m.id,
           jsonb_agg(
               jsonb_set(t.meldeperiode, '{brukersMeldekortIder}', coalesce(n.brukers_meldekort_ider, '[]'::jsonb))
               ORDER BY t.ordinalitet
           ) AS meldeperioder
    FROM meldekortbehandling m
    CROSS JOIN jsonb_array_elements(m.meldeperioder) WITH ORDINALITY AS t(meldeperiode, ordinalitet)
    LEFT JOIN ny_liste n
        ON n.meldekortbehandling_id = m.id
        AND n.kjede_id = t.meldeperiode ->> 'kjedeId'
    WHERE m.id IN (SELECT meldekortbehandling_id FROM ny_kobling)
    GROUP BY m.id
)
UPDATE meldekortbehandling m
SET meldeperioder = n.meldeperioder
FROM nye_meldeperioder n
WHERE m.id = n.id;
