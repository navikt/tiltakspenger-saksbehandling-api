-- vedtak_id i statistikk_utbetaling skal alltid peke på rammevedtakene som er bakgrunnen for utbetalingen.
-- Siden august 2025 har utbetalinger fra meldekortvedtak fått meldekortvedtakets egen id; disse radene byttes ut med rammevedtakene til meldekortbehandlingen.
-- Rekkefølgen følger Meldeperiodebehandlinger.rammevedtakIder: meldeperiodene i behandlingen, og innen hver meldeperiode periodene i rammevedtak-periodiseringen, uten duplikater.
-- Rader som allerede peker på rammevedtak, eller som mangler vedtak_id, røres ikke.
WITH rammevedtak_per_meldekortvedtak AS (
    SELECT mv.id                                                  AS meldekortvedtak_id,
           jsonb_agg(r.vedtak_id ORDER BY r.meldeperiode_nr, r.periode_nr) AS rammevedtak_ider
    FROM meldekortvedtak mv
    JOIN meldekortbehandling mb ON mb.id = mv.meldekort_id
    CROSS JOIN LATERAL (
        SELECT DISTINCT ON (p.verdi ->> 'vedtakId')
               p.verdi ->> 'vedtakId' AS vedtak_id,
               mpb.nr                 AS meldeperiode_nr,
               p.nr                   AS periode_nr
        FROM jsonb_array_elements(mb.meldeperioder) WITH ORDINALITY AS mpb(verdi, nr)
        JOIN meldeperiode mp ON mp.id = mpb.verdi ->> 'meldeperiodeId'
        CROSS JOIN LATERAL jsonb_array_elements(mp.rammevedtak -> 'perioderTilVedtakId') WITH ORDINALITY AS p(verdi, nr)
        WHERE p.verdi ->> 'vedtakId' IS NOT NULL
        ORDER BY p.verdi ->> 'vedtakId', mpb.nr, p.nr
    ) r
    GROUP BY mv.id
)
UPDATE statistikk_utbetaling su
SET vedtak_id   = rv.rammevedtak_ider,
    sist_endret = now()
FROM rammevedtak_per_meldekortvedtak rv
WHERE su.vedtak_id = jsonb_build_array(rv.meldekortvedtak_id);
