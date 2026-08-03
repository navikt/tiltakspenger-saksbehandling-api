# AGENTS.md — tiltakspenger-saksbehandling-api

Dette repoet følger monorepo-konvensjonene i [`../AGENTS.md`](../AGENTS.md) og Kotlin/JVM-backendkonvensjonene i [`../AGENTS-backend.md`](../AGENTS-backend.md).
Les disse først.

## Kodestil

- **KDoc og kommentarer: én setning per linje.**
  Hver setning i KDoc (`/** ... */`) og vanlige kommentarer skal stå på sin egen linje, med linjeskift etter punktum, i stedet for flere setninger pakket sammen på én lang linje.
  Dette gir renere diffs og bedre lesbarhet.
  Gjelder også fler-setnings `//`-kommentarer.
  Se den utfyllende regelen i [`../AGENTS-backend.md`](../AGENTS-backend.md#språk-og-stil) — den gjentas her fordi agenter glipper på den gang på gang.

## Pakkestruktur

- **Databaserelaterte filer ligger under `<domene>/infra/repo/`.**
  Db-mapping hører til domenet den lagrer, ikke til en delt db-pakke.
  Portene (repo-grensesnittene) ligger i `<domene>/ports/`, implementasjonen i `<domene>/infra/repo/`.
- **`felles` er domenekode, `infra` er infrastruktur.**
  De to pakkene ligger side om side rett under `no.nav.tiltakspenger.saksbehandling`, og `felles` skal verken ha en `infra`-underpakke eller importere infrastruktur.
  Mapping av en felles domenetype er infrastruktur selv om typen den mapper er felles, og hører derfor hjemme under `infra` — ikke ved siden av typen.
  Håndheves av `FellesErDomenepakkeKonsistTest`, som har en whitelist-ratchet for det som gjenstår.

## Testtaksonomi

Taksonomien — prodstier, aggregat-disiplin og filter-krykka som motbilde — står i [`../AGENTS-backend.md`](../AGENTS-backend.md#testtaksonomi-prodstier-og-aggregat-disiplin).
Slik ser den ut i dette repoet:

- **Prodstien er `withTestApplicationContextAndPostgres`** i `common/TestApplicationContextEx.kt`, sammen med route-byggerne.
- **Aggregat-tester** merkes med `@IsolatedDatabaseTest` og `runIsolated = true`.
  Konvensjonen om at de to alltid følges ad, håndheves av `IsolatedDatabaseTestKonvensjonTest`.
  `MeldekortvedtakAggregatTest` er mønsterfila.
- **`AggregatspørringKonsistTest` håndhever punkt 3 i taksonomien:** `hent*(limit)`-metodene på repo-portene kalles kun fra `*AggregatTest`-filer og fake-repoer, og `Int.MAX_VALUE` skal aldri sendes som limit.
  Metodenavnene utledes fra `ports/`-pakkene, så en ny jobbspørring dekkes automatisk.
  Whitelisten i fila er arbeidslista for det som gjenstår, og en fil som er ryddet må ut av den — testen feiler ellers.
- **Jobber som følger en iverksettelse slås av og på med `JobberEtterIverksettelse`** i route-byggerne.
  Skal en test observere en kø, må jobben som tømmer køen slås av — ellers er køen alltid tom når testen ser på den.
- **Negative databasetester** ligger i `*NegativTest`-filer; `MeldekortvedtakPostgresRepoNegativTest` er mønsteret.
- **Rene db-typer uten domeneflyt** er den andre unntakskategorien; `PeriodeDbTest` er eksempelet her.
- **Dekningsgaten krever 100 % både linje- og grendekning (`CoverageUnit.LINE` og `CoverageUnit.BRANCH`) på hele databaselaget** — se `databaselagMedDekningskrav` i `build.gradle.kts`.
  Hver gren i et vilkår må altså være truffet begge veier av testene — nullable felter begge veier, `rowsAffected > 0` både med og uten treff, og hver `when`-arm.
  De to gatene står side om side fordi de fanger ulike hull: full grendekning sier ingenting om en linje uten grener, og full linjedekning sier ingenting om hvilken vei et vilkår ble tatt.
  Gaten er mønsterbasert og består av to mønstre: alt som ligger under en `infra/repo`-pakke uansett dybde (inkludert `*DbJson`-filene), og alt som heter `*Repo` uansett hvor det ligger.
  Kover matcher på fullt klassenavn, og `*` dekker også punktum — det finnes ingen `**`, og det trengs ikke.
  Ny kode i databaselaget er altså dekket som standard, og skal ikke legges til for hånd.
  Eneste unntak er `DataSourceSetup` og `FlywayMigrate`, som er bootstrap og ikke databaselag; de står i `bootstrapUtenforDekningskravet` med begrunnelse.
  Merk at kover-rapportene ikke lenger genereres av `check`; kjør `./gradlew koverHtmlReport` eksplisitt når du skal lese dem.
- **Testhjelpere som ikke hører i prodkoden ligger i `*TestEx.kt` ved siden av typen de gjelder.**
  `BegrunnelseTestEx.kt` og `BarnetilleggTestEx.kt` er bekvemmelighetskonstruktører som companion-extensions, `StatistikkTestEx.kt` og `TiltaksdeltakerHendelseTestEx.kt` er databaseoppslag kun tester trenger.
  Repoet har null `@TestOnly` i `src/main`, og det skal det fortsette å ha.
- **Rene mappinger testes som enhetstester som pinner den lagrede strengen eller json-en**, ikke bare rundturen — se «Row hører i databasetesten, ren mapping i enhetstesten» i [`../AGENTS-backend.md`](../AGENTS-backend.md).
  `HjemmelForOpphørDbTest` og `TiltakDeltakerstatusDbTest` er mønsterfilene.

> **Overgangsfase (per 2026-08-03):** kun `BenkOversiktAggregatTest` bygger fortsatt tilstand via `withMigratedDb` og `TestDataHelper`.
> Det universet er under avvikling, og du skal ikke utvide det.
> Trenger du ny testtilstand, bygg den gjennom prodstiene.
> Unntaket er `PeriodeDbTest`, som blir stående — den tester en ren db-type og har ingen prodsti å bygges gjennom.

## Lokal testdata

- Hvordan du oppretter en meldekortbehandling og en klagebehandling lokalt (scripts, curl og GUI fra A til Å, både digital og papir søknad) er dokumentert i frontend-repoet: [`../tiltakspenger-saksbehandling/docs/opprette-behandlinger-lokalt.md`](../tiltakspenger-saksbehandling/docs/opprette-behandlinger-lokalt.md).
- Kjørbare script ligger i [`../tiltakspenger-saksbehandling/scripts/testdata/`](../tiltakspenger-saksbehandling/scripts/testdata/) og kjører mot `LokalMain` på `http://localhost:8080`.
