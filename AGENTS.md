# AGENTS.md — tiltakspenger-saksbehandling-api

Dette repoet følger monorepo-konvensjonene i [`../AGENTS.md`](../AGENTS.md) og Kotlin/JVM-backendkonvensjonene i [`../AGENTS-backend.md`](../AGENTS-backend.md).
Les disse først.

## Kodestil

- **KDoc og kommentarer: én setning per linje.**
  Hver setning i KDoc (`/** ... */`) og vanlige kommentarer skal stå på sin egen linje, med linjeskift etter punktum, i stedet for flere setninger pakket sammen på én lang linje.
  Dette gir renere diffs og bedre lesbarhet.
  Gjelder også fler-setnings `//`-kommentarer.
  Se den utfyllende regelen i [`../AGENTS-backend.md`](../AGENTS-backend.md#språk-og-stil) — den gjentas her fordi agenter glipper på den gang på gang.

## Testtaksonomi

Taksonomien — prodstier, aggregat-disiplin og filter-krykka som motbilde — står i [`../AGENTS-backend.md`](../AGENTS-backend.md#testtaksonomi-prodstier-og-aggregat-disiplin).
Slik ser den ut i dette repoet:

- **Prodstien er `withTestApplicationContextAndPostgres`** i `common/TestApplicationContextEx.kt`, sammen med route-byggerne.
- **Aggregat-tester** merkes med `@IsolatedDatabaseTest` og `runIsolated = true`.
  Konvensjonen om at de to alltid følges ad, håndheves av `IsolatedDatabaseTestKonvensjonTest`.
  `MeldekortvedtakAggregatTest` er mønsterfila.
- **Jobber som følger en iverksettelse slås av og på med `JobberEtterIverksettelse`** i route-byggerne.
  Skal en test observere en kø, må jobben som tømmer køen slås av — ellers er køen alltid tom når testen ser på den.
- **Negative databasetester** ligger i `*NegativTest`-filer; `MeldekortvedtakPostgresRepoNegativTest` er mønsteret.
- **Rene db-typer uten domeneflyt** er den andre unntakskategorien; `PeriodeDbTest` er eksempelet her.
- **Dekningsgaten** låser repoene som har nådd 100 % — se `postgresRepoerMedDekningskrav` i `build.gradle.kts`.
  Merk at kover-rapportene ikke lenger genereres av `check`; kjør `./gradlew koverHtmlReport` eksplisitt når du skal lese dem.

> **Overgangsfase (per 2026-07-30):** 20 testfiler bygger fortsatt tilstand via `withMigratedDb` og `TestDataHelper`.
> Det universet er under avvikling, og du skal ikke utvide det.
> Trenger du ny testtilstand, bygg den gjennom prodstiene.

## Lokal testdata

- Hvordan du oppretter en meldekortbehandling og en klagebehandling lokalt (scripts, curl og GUI fra A til Å, både digital og papir søknad) er dokumentert i frontend-repoet: [`../tiltakspenger-saksbehandling/docs/opprette-behandlinger-lokalt.md`](../tiltakspenger-saksbehandling/docs/opprette-behandlinger-lokalt.md).
- Kjørbare script ligger i [`../tiltakspenger-saksbehandling/scripts/testdata/`](../tiltakspenger-saksbehandling/scripts/testdata/) og kjører mot `LokalMain` på `http://localhost:8080`.
