-- Historiske rader i statistikk_sak for manuelle meldekortbehandlinger, bestilt av Team Sak i #tiltakspenger-dvh oktober 2026.
-- Feltene følger SaksstatistikkMeldekortbehandlingMapper; SaksstatistikkMeldekortbehandlingTest sammenligner radene herfra med radene prodkoden lager.
-- Bare hendelser databasen kan gjenskape får rader: opprettet, siste send til beslutter, underkjenninger, iverksetting og avbrudd.
-- Bare databasen leses, så migreringen gjør ingen kall mot PDL; strengt fortrolig adresse maskeres når saken ikke har noen umaskerte rader fra før.
--
-- Ingen hendelse kan få to rader: har behandlingen allerede rader fra prodkoden, settes bare hendelser av andre typer inn, og bare de som skjedde før den første raden.
-- Vi kan ikke sammenligne på tidspunkt, fordi prodkoden bruker sistEndret, som leses fra klokka rett etter sendt_til_beslutning og attesteringen.
-- Prisen er at en underkjenning fra før deploy mangler hvis behandlingen også er underkjent etterpå.
WITH umaskert AS (
    SELECT DISTINCT sak_id FROM statistikk_sak WHERE opprettetav <> '-5'
),
manuell AS (
    SELECT m.*,
           s.saksnummer,
           s.fnr,
           u.sak_id IS NULL AS maskert,
           CASE
               WHEN jsonb_path_exists(m.meldeperioder, '$[*] ? (@.type != "KORRIGERING")') THEN 'MELDEKORT'
               ELSE 'KORRIGERING'
           END AS aarsak
    FROM meldekortbehandling m
    JOIN sak s ON s.id = m.sak_id
    LEFT JOIN umaskert u ON u.sak_id = m.sak_id
    WHERE m.status <> 'AUTOMATISK_BEHANDLET'
),
relatert AS (
    SELECT m.id,
           (
               SELECT rv.behandling_id
               FROM meldeperiode mp
               CROSS JOIN LATERAL jsonb_array_elements(mp.rammevedtak -> 'perioderTilVedtakId') p
               JOIN rammevedtak rv ON rv.id = p ->> 'vedtakId'
               WHERE mp.id = m.meldeperioder -> 0 ->> 'meldeperiodeId'
               ORDER BY (p -> 'periode' ->> 'fraOgMed')::date
               LIMIT 1
           ) AS behandling_id
    FROM manuell m
),
mottatt AS (
    SELECT m.id,
           min(
               CASE
                   WHEN b.behandles_automatisk THEN m.opprettet
                   ELSE least(coalesce((b.behandlet_automatisk_metadata ->> 'forrigeForsøk')::timestamptz, b.mottatt), m.opprettet)
               END
           ) AS tidspunkt
    FROM manuell m
    CROSS JOIN LATERAL jsonb_array_elements(m.meldeperioder) mp
    JOIN meldekort_bruker b
        ON b.sak_id = m.sak_id
       AND b.meldeperiode_kjede_id = mp ->> 'kjedeId'
       AND b.mottatt <= m.opprettet
    WHERE NOT EXISTS (
        SELECT 1
        FROM meldekortbehandling annen
        WHERE annen.sak_id = m.sak_id
          AND annen.id <> m.id
          AND annen.avbrutt IS NULL
          AND annen.meldeperioder @> jsonb_build_array(jsonb_build_object('kjedeId', mp ->> 'kjedeId'))
          AND annen.opprettet >= b.mottatt
          AND annen.opprettet < m.opprettet
    )
    GROUP BY m.id
),
hendelse AS (
    SELECT id, opprettet AS endret, 'opprettet_behandling' AS hendelse, 'UNDER_BEHANDLING' AS status,
           NULL AS resultat, NULL AS beslutter, NULL::timestamptz AS ferdig, NULL::timestamptz AS vedtak
    FROM manuell
    UNION ALL
    SELECT id, sendt_til_beslutning, 'sendt_til_beslutter', 'UNDER_BESLUTNING', NULL, NULL, NULL, NULL
    FROM manuell
    WHERE sendt_til_beslutning IS NOT NULL
    UNION ALL
    SELECT m.id, (a ->> 'tidspunkt')::timestamptz, 'underkjent_behandling', 'UNDER_BEHANDLING', NULL, a ->> 'beslutter', NULL, NULL
    FROM manuell m
    CROSS JOIN LATERAL jsonb_array_elements(m.attesteringer -> 'attesteringer') a
    WHERE a ->> 'status' = 'SENDT_TILBAKE'
    UNION ALL
    SELECT id, iverksatt_tidspunkt, 'iverksatt_behandling', 'FERDIG_BEHANDLET', 'GODKJENT', beslutter, iverksatt_tidspunkt, iverksatt_tidspunkt
    FROM manuell
    WHERE status = 'GODKJENT'
    UNION ALL
    SELECT id, (avbrutt ->> 'avbruttTidspunkt')::timestamptz, 'avsluttet_behandling', 'AVSLUTTET', 'AVBRUTT', beslutter,
           (avbrutt ->> 'avbruttTidspunkt')::timestamptz, NULL
    FROM manuell
    WHERE avbrutt IS NOT NULL
),
eksisterende AS (
    SELECT behandlingid, min(endrettidspunkt) AS forste, array_agg(DISTINCT hendelse) AS hendelser
    FROM statistikk_sak
    WHERE behandlingtype = 'MELDEKORT'
    GROUP BY behandlingid
)
INSERT INTO statistikk_sak (
    sak_id, saksnummer, behandlingid, relatertbehandlingid, fnr,
    mottatt_tidspunkt, registrerttidspunkt, ferdigbehandlettidspunkt, vedtaktidspunkt, utbetalttidspunkt, endrettidspunkt,
    soknadsformat, forventetoppstarttidspunkt, teknisktidspunkt, sakytelse,
    behandlingtype, behandlingstatus, behandlingresultat, resultatbegrunnelse, behandlingmetode,
    opprettetav, saksbehandler, ansvarligbeslutter,
    tilbakekrevingsbelop, funksjonellperiode_fra_og_med, funksjonellperiode_til_og_med,
    hendelse, avsender, versjon, behandling_aarsak, relatertfagsystem, sakutland, ansvarligenhet
)
SELECT m.sak_id, m.saksnummer, m.id, r.behandling_id, m.fnr,
       coalesce(mo.tidspunkt, m.opprettet), m.opprettet, h.ferdig, h.vedtak, NULL, h.endret,
       CASE WHEN mo.tidspunkt IS NULL THEN 'ANNET' ELSE 'DIGITAL' END, NULL, clock_timestamp(), 'IND',
       'MELDEKORT', h.status, h.resultat, NULL, 'MANUELL',
       CASE WHEN m.maskert THEN '-5' ELSE 'system' END,
       CASE WHEN m.maskert AND m.saksbehandler IS NOT NULL THEN '-5' ELSE m.saksbehandler END,
       CASE WHEN m.maskert AND h.beslutter IS NOT NULL THEN '-5' ELSE h.beslutter END,
       NULL, NULL, NULL,
       h.hendelse, 'tiltakspenger-saksbehandling-api', 'V262', m.aarsak, 'TPSAK', 'NASJONAL',
       CASE WHEN m.maskert THEN '-5' ELSE '0387' END
FROM hendelse h
JOIN manuell m ON m.id = h.id
LEFT JOIN relatert r ON r.id = m.id
LEFT JOIN mottatt mo ON mo.id = m.id
LEFT JOIN eksisterende e ON e.behandlingid = m.id
WHERE e.behandlingid IS NULL
   OR (h.endret < e.forste AND NOT (h.hendelse = ANY (e.hendelser)))
ORDER BY h.endret, m.id;
