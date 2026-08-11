-- Indekser for benk v2-spørringene i BenkV2PostgresRepo.
--
-- Alle fem fanene filtrerer på "åpne" behandlinger, men ingen av tabellene hadde indeks på statusfiltrene.
-- Hvert benk-kall kjører basespørringene flere ganger (to counts + rader for aktiv fane, pluss én count per fane for fane-tellerne),
-- så hver sidevisning gjorde åtte fulle tabellskann.
-- Målt mot Postgres 17 med 100k behandling / 50k meldekortbehandling / 20k klage / 20k tilbakekreving:
-- hentAntallPerFane gikk fra 143ms til under 10ms med disse indeksene.
--
-- Predikatet er `avbrutt is null` framfor benkens statusliste, slik at indeksen overlever endringer i hvilke statuser benken viser.
-- Postgres utleder at `status in (...)` i spørringen treffer via indekskolonnen, så begge benk-fanene deler indeksen på behandling.
-- Klagebehandling og tilbakekreving_behandling har ingen avbrutt-kolonne, så der er statuslisten predikatet i stedet.

CREATE INDEX idx_behandling_benk_apen
    ON behandling (behandlingstype, status)
    WHERE avbrutt IS NULL;

CREATE INDEX idx_meldekortbehandling_benk_apen
    ON meldekortbehandling (status)
    WHERE avbrutt IS NULL;

CREATE INDEX idx_klagebehandling_benk_apen
    ON klagebehandling (status)
    WHERE status IN ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'MOTTATT_FRA_KLAGEINSTANS');

CREATE INDEX idx_tilbakekreving_behandling_benk_apen
    ON tilbakekreving_behandling (status)
    WHERE status IN ('OPPRETTET', 'TIL_FORHÅNDSVARSEL', 'TIL_BEHANDLING', 'TIL_GODKJENNING');
