-- Indeks for meldekortene fra bruker som venter på behandling i benken (MELDEKORT_FRA_BRUKER i BenkPostgresRepo).
-- Den gamle indeksen dekket alle meldekort som ikke ble behandlet automatisk, inkludert de som er ferdig behandlet manuelt, og vokste derfor med all historikk.
-- Med behandling_id i predikatet forsvinner et meldekort fra indeksen når det behandles, så indeksen holder seg liten.
-- behandlet_automatisk_status er ikke med, siden et automatisk behandlet meldekort alltid har behandling_id.
-- I prod 27.11.2026 fantes ingen automatisk behandlede meldekort uten kobling fra en automatisk behandling, så det gjelder også etter V288.
-- Målt mot Postgres 17 med 37k meldekort som ikke ble behandlet automatisk, hvorav 50 ubehandlede: 73 ms før, 1 ms etter.
CREATE INDEX IF NOT EXISTS idx_meldekort_bruker_ubehandlet_kjede_mottatt
    ON meldekort_bruker (sak_id, meldeperiode_kjede_id, mottatt DESC)
    WHERE behandling_id IS NULL AND behandles_automatisk = false;

-- Benk-spørringen var den eneste som brukte den gamle indeksen.
DROP INDEX IF EXISTS idx_meldekort_bruker_apent_kjede_mottatt;
