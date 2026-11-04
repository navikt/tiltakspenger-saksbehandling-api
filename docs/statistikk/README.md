# Statistikk til datavarehuset (DVH)

Saksbehandling-api skriver statistikk til fire tabeller og et kodeverk som bare datavarehuset leser.
Prodkoden vår leser dem aldri, så en feil her synes ikke hos oss, men gir feil tall hos Team Sak eller Team Spenn.
Les denne fila før du endrer en statistikktabell, en DTO under `statistikk/` eller hendelsene som skrives.

## Tabellene

| Tabell | Leses av | Innhold | Skrives slik |
| --- | --- | --- | --- |
| `statistikk_sak` | Team Sak | Én rad per hendelse på en rammebehandling, klagebehandling eller manuell meldekortbehandling | Bare nye rader |
| `statistikk_stonad` | Team Spenn | Én rad per rammevedtak | Bare nye rader |
| `statistikk_utbetaling` | Team Spenn | Én rad per utbetaling | Bare nye rader |
| `statistikk_meldekort` | Team Spenn | Én rad per iverksatt meldekortbehandling, både manuell og automatisk | Upsert på `meldekortbehandling_id` |
| `statistikk_kodeverk_tiltakstype` | Team Spenn | Én rad per ekstern tiltakskode som gir rett og stønadstypen vi sender til helved for den, med periode for når mappingen gjaldt hos oss | Synkroniseres fra koden, se under |

KDoc på DTO-ene beskriver feltene: `SaksstatistikkDTO`, `StatistikkStønadDTO`, `StatistikkUtbetalingDTO` og `StatistikkMeldekortDTO`.
`SaksstatistikkDTO` lenker også til grensesnittbeskrivelsen Team Sak eier.

## Slik skrives radene

1. Domenefunksjonen som endrer en behandling, returnerer `Statistikkhendelser` sammen med den endrede behandlingen.
2. Servicen kaller `StatistikkService.generer` før transaksjonen, fordi den slår opp strengt fortrolig adresse i PDL.
3. Servicen lagrer statistikken med `StatistikkService.lagre` i samme transaksjon som behandlingen, så statistikken aldri kan avvike fra det som ble lagret.

Hvilke hendelser som gir rader i `statistikk_sak`, står i `StatistikkhendelseType`, og mapperne ligger i `statistikk/saksstatistikk/rammebehandling/`, `klagebehandling/` og `meldekortbehandling/`.
Databasen setter `teknisktidspunkt` med `clock_timestamp()` ved insert, slik at rekkefølgen følger `id`.
Radene oppdateres aldri, med to unntak: `SaksstatistikkPostgresRepo.oppdaterFnr` ved nytt fødselsnummer og `oppdaterAdressebeskyttelse` ved strengt fortrolig adresse.

## Slik skrives kodeverket

`statistikk_kodeverk_tiltakstype` bygges fra `eksterneTiltakskoderSomGirRett` i tiltakspenger-libs og `mapStønadstype()`, de samme kildene som klassifiseringen og utbetalingen bruker, så kodeverket kan ikke avvike fra det vi faktisk sender.

- Jobben `saksbehandling-jobb-synkroniser-statistikk-kodeverk-tiltakstype` kjører én gang per pod på lederen, se `StatistikkService.synkroniserTiltakstypekodeverk`.
  Én gang per pod hindrer at en gammel pod under utrullingen skriver tilbake den gamle mappingen.
- En mapping som forsvinner eller får ny stønadstype, får `gyldig_til` og ny `sist_endret`, og en ny eller endret mapping får en ny rad med `gyldig_fra`.
- Radene fra første synkronisering har `gyldig_fra = null`, fordi mappingen gjaldt før kodeverket fantes.
- Klassekoden i UR er stønadstypen kombinert med barnetillegg, og den kombinasjonen gjør helved; vi kan bare stå inne for stønadstypen.

## Slik kommer dataene til DVH

- Datastream-strømmen `postgres-saksbehandling-bigquery` replikerer tabellene til BigQuery-datasettet `datastream_saksbehandling` i tpts-prosjektet, med om lag 15 minutters forsinkelse.
- Strømmen har en fast tabelliste, så en ny tabell må legges til der i både dev og prod; nye kolonner i en tabell som allerede er med, kommer med av seg selv.
- Migreringene V236 og V237 og `db/callback/afterMigrate__gjenopprett_replication_slot.sql` setter opp publikasjonen og replikeringsslotten.
- Stopper strømmen, holder replikeringsslotten på WAL-en slik at disken vokser; se navikt/tiltakspenger-saksbehandling-api#1825.

## Slik leser Team Sak `statistikk_sak`

Koden ligger i [navikt/dvh_saksbehandlingsstatistikk](https://github.com/navikt/dvh_saksbehandlingsstatistikk) (internt repo), i filer med `tiltakspenger` i navnet.
Lenker til grensesnittbeskrivelsen og Team Saks dokumentasjon står i KDoc på `SaksstatistikkDTO`.

- De leser BigQuery-viewet `statistikk_sak_dvh` i `datastream_saksbehandling`, ikke tabellen direkte; definisjonen av viewet ligger ikke som kode i noe repo.
- Naisjob-en `pt-sak-tiltakspenger` kjører fem over hver hele time og henter bare rader med `id` høyere enn den største de har fra før.
  De ser derfor ikke endringer i eksisterende rader; alt som skal fram, må være en ny rad.
- De beholder én rad per `behandlingid` + `endrettidspunkt`, den med nyeste `teknisktidspunkt` (`tiltakspenger_behandling_filt`).
  Resending og retting bygger på dette, se [resending_team_sak.md](resending_team_sak.md); derfor kutter vi heller ikke tidspunktene til hele sekunder.
- De velger siste tilstand per behandling etter nyeste `teknisktidspunkt` (`tiltakspenger_behandling_latest`).
- Team Sak slår opp behandlingstype, status, resultat og årsak i egne kodetabeller for kildesystemet `TILTAKSP`.
  En ny verdi som ikke finnes der, havner som ukjent, så avklar nye verdier med Team Sak før du tar dem i bruk.
- Strengt fortrolig adresse maskerer de selv ut fra egne persondata; vår `-5` er en ekstra sikring.

## Slik leser Team Spenn de tre andre tabellene

`statistikk_kodeverk_tiltakstype` heter `public_statistikk_kodeverk_tiltakstype` i BigQuery; Team Spenn har lesetilgang, men leser den ikke i `datalast_fra_bq.py` ennå.
Den er også publisert som datasett på datamarkedsplassen, se under.

Koden ligger i [navikt/dvh_tiltakspenger](https://github.com/navikt/dvh_tiltakspenger) (internt repo).

- `etl/datalast_fra_bq.py` henter alle radene fra `public_statistikk_stonad`, `public_statistikk_utbetaling` og `public_statistikk_meldekort` hver natt med en fast kolonneliste, og laster dem på nytt.
- dbt-modellene under `dbt/models` bygger historikk og fakta videre fra disse radene.
- En ny kolonne kommer ikke med før Team Spenn legger den inn i kolonnelista; en kolonne vi fjerner, må først tas ut av lista.

## Tilganger og dataprodukter i BigQuery

Ingenting av dette ligger som kode; det er satt opp i GCP-konsollen, med `bq` eller fra datamarkedsplassen (lest 2026-11-03).
Alt ligger i tpts-prosjektet, og tabellene i `datastream_saksbehandling` har prefikset `public_`.

| Objekt i prod | Hvem leser | Hvorfor |
| --- | --- | --- |
| `datastream_saksbehandling.public_statistikk_stonad`, `_utbetaling`, `_meldekort` og `_kodeverk_tiltakstype` | Team Spenns tjenestekonto `tiltakspenger-bq` og enkeltpersoner i Team Spenn | Nattlig innlesing til DVH |
| `datastream_saksbehandling.public_statistikk_sak` og viewet `statistikk_sak_dvh` | Team Saks tjenestekonto `tiltakspenger-ptsak-reader-pts` og én person i Team Sak | Timesjobben `pt-sak-tiltakspenger` |
| `markedsplassen_pseudo.datastream_saksbehandling_public_statistikk_sak` og `_meldekort` | Datamarkedsplassen | Dataprodukter med fødselsnummer og `bruker_id` pseudonymisert med nytt salt per spørring, så de kan ikke kobles mellom spørringer |
| `views.meldekort_til_infoskjerm` og `views.utbetalinger_til_infoskjerm` | Grafana | Infoskjerm uten personopplysninger |

Datamarkedsplassen har i tillegg direkte lesetilgang til `public_statistikk_meldekort` og `public_statistikk_utbetaling`.
Teamet har ett dataprodukt der, «Tiltakspenger», med datasettene Meldekort for tiltakspenger, Tiltakspengesaker, begge også pseudonymisert, Tiltakspengevedtak, Utbetalingsvedtak for tiltakspenger og [Kodeverk for tiltakstyper](https://data.ansatt.nav.no/dataproduct/f5ecf970-61d3-4748-b2cc-432166cae1ae/Tiltakspenger/11e85edc-9283-4a32-8f85-25e31e37576c).
Kodeverket er åpent for hele Nav, siden det ikke har personopplysninger.
Hvem som har fått tilgang gjennom dataproduktet, ser du bare på [datamarkedsplassen](https://data.ansatt.nav.no) under teamet.

I dev har Team Spenn og Team Sak lesetilgang til hele `datastream_saksbehandling`.

Slik er tilgangen bygd opp i prod:

- Lesetilgang gis per tabell med `roles/bigquery.dataViewer`, ikke på datasettet, så en konsument ser bare tabellene den har fått.
- Datasettet gir konsumenten bare `roles/bigquery.metadataViewer`, slik at tabellene vises.
- Et view i et annet datasett må stå som autorisert view på `datastream_saksbehandling`, ellers får leseren ingen data.

Slik endrer du:

- Gi tilgang: `bq add-iam-policy-binding --member=serviceAccount:<konto> --role=roles/bigquery.dataViewer tpts-prod-b5ff:datastream_saksbehandling.<tabell>`.
  En ny konsument trenger også `metadataViewer` på datasettet.
- Gi heller tilgang til en tjenestekonto enn til personer, og send personopplysninger til andre enn Team Sak og Team Spenn gjennom et pseudonymisert dataprodukt på datamarkedsplassen.
- Fjern tilgang når noen slutter eller bytter team, både på hver tabell (`bq remove-iam-policy-binding`) og i datasettets tilgangsliste (`bq show --format=json`, fjern oppføringen, `bq update --source`), i dev og prod.
- Les tilgangene tilbake med `bq get-iam-policy` på tabellen og `bq show` på datasettet etterpå.

## Før du endrer noe

- Ny hendelse eller ny verdi i `statistikk_sak`: avklar med Team Sak i `#tiltakspenger-dvh` og legg verdien inn i enumen og KDoc.
- Ny eller endret kolonne: avklar med teamet som leser tabellen, og oppdater KDoc på DTO-en.
- Fjernet kolonne: spør konsumenten, og fjern den først når de har sluttet å lese den.
  Datastream fjerner ikke kolonnen i BigQuery, den blir bare tom, så konsumenten får ingen feil og mister dataene uten å merke det.
- Ny tabell: legg den inn i Datastream-strømmen i begge miljøer, og gi konsumenten tilgang som beskrevet over.
- Historiske rader: skriv en ren SQL-migrering som bare leser databasen, med vern mot dobbeltrader og en tørrkjøring i prod først; `V264__statistikk_sak_historiske_meldekortbehandlinger.sql` er mønsteret.
- Retter du eksisterende rader samtidig som du endrer koden som skriver dem, kan den gamle poden skrive rader med gammel kode etter at migreringen har kjørt, fordi den er leder til den nye har tatt over; slik fikk fire rader i `statistikk_utbetaling` meldekortvedtakets id etter V265, og oppdateringen måtte kjøres på nytt.
  Kontroller radene fra utrullingen etterpå, eller lever kodeendringen før rettingen.
- Tester: test insert-SQL og felt ved å lese radene tilbake med egen SQL, se `StatistikkLagringTest` og `SaksstatistikkMeldekortbehandlingTest`.

## Det vi ikke skriver

- Utfall fra Klageinstansen: Team Sak leser klagebehandlingen direkte fra Kabal, så `statistikk_sak` stopper ved `klagebehandling_oversendt_ka`.
- Tilbakekreving: Team Sak får statistikken fra tilbakeløsningen; de kobler den til våre behandlinger gjennom relatert behandlings-id.
- Automatiske meldekortbehandlinger i `statistikk_sak`: Team Sak har bare bedt om de manuelle.
