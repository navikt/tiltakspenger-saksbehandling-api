tiltakspenger-saksbehandling-api
================

Håndterer vedtak om [tiltakspenger](https://www.nav.no/no/person/arbeid/oppfolging-og-tiltak-for-a-komme-i-jobb/stonader-ved-tiltak).
Backend for [tiltakspenger-saksbehandling](https://github.com/navikt/tiltakspenger-saksbehandling).

En del av satsningen ["Flere i arbeid – P4"](https://memu.no/artikler/stor-satsing-skal-fornye-navs-utdaterte-it-losninger-og-digitale-verktoy/)


# Komme i gang
## Forutsetninger
- [JDK](https://jdk.java.net/)
- [Kotlin](https://kotlinlang.org/)
- [Gradle](https://gradle.org/) brukes som byggeverktøy og er inkludert i oppsettet


## Lokal kjøring

Appen kan kjøres opp lokalt med `./gradlew :lokal:run`, eller med `main`-funksjonen i [LokalMain.kt](lokal/src/main/kotlin/no/nav/tiltakspenger/saksbehandling/lokal/LokalMain.kt).
Denne bruker fakes for de fleste tjenester, og krever en lokal database og autentiseringstjenestene i Docker.

Tjenestene kan startes med docker-compose i [meta-repoet for tiltakspenger](https://github.com/navikt/tiltakspenger).
Se README der for fremgangsmåte og import av data.

Compose kan også kjøre selve API-et.
`./gradlew :lokal:installDist` bygger distribusjonen som [lokal/Dockerfile](lokal/Dockerfile) pakker, med `lokal` som byggekontekst.
Imaget har eget inngangspunkt i [LokalComposeMain.kt](lokal/src/main/kotlin/no/nav/tiltakspenger/saksbehandling/lokal/LokalComposeMain.kt), som bruker ekte klienter mot tjenestene i Compose.
Compose-oppsettet i metarepoet og API-repoet må oppdateres sammen, fordi Compose peker på denne modulen og distribusjonen.

## Kover Report

For å se en oversikt over testdekningen kan du generere en rapport med:
```
./gradlew koverHtmlReport
```
Rapporten havner da under:

```
build/reports/kover/html/index.html
```

## Spørringer mot database
GCP-databasene (dev eller prod) kan nåes fra lokal maskin på ulike måter: 

Med [nais cli](https://docs.nais.io/persistence/postgres/how-to/personal-access/). Se doc'en for førstegangsoppsett, senere kan du kjøre disse kommandoene:
```
kubectl config use-context (dev|prod)-gcp
nais postgres proxy [-p <port>] tiltakspenger-saksbehandling-api
```

Med [Cloud SQL Proxy](https://cloud.google.com/sql/docs/postgres/sql-proxy)
```
cloud_sql_proxy -instances=tpts-dev-6211:europe-north1:tiltakspenger-saksbehandling-api=tcp:5432 -enable_iam_login
```

Eventuelt med [Cloud SQL Studio](https://console.cloud.google.com/sql/instances/tiltakspenger-saksbehandling-api/studio) web UI.

---
# Konvensjoner

## Moduler

Rota er produksjonsappen, og produksjonsimaget bygges fra `Dockerfile` i rota.
`dev` er en egen Gradle-modul for utviklingsimaget, med eget inngangspunkt i `no.nav.tiltakspenger.saksbehandling.dev`, `dev/Dockerfile` og `dev/.dockerignore`.
`dev` avhenger av rota, og produksjonskoden i rota avhenger aldri av `dev`.
`lokal` er en egen Gradle-modul for lokal kjøring.
Den avhenger av rota, `dev` og rotas testfixtures i `src/testFixtures`, og verken rotas hovedkode eller `dev` avhenger av `lokal`.
I rota er `dev` og `lokal` bare med på testenes classpath.
Testene kompilerer mot `dev`, mens `lokal` bare er med når de kjører.
`ProdConfig` brukes i `prod-gcp`.
Ellers lastes konfigurasjonen med `ServiceLoader` i Java, og oppstarten krever nøyaktig én leverandør for klyngen.
`DevConfig` for dev-gcp ligger i `dev`, og `LocalConfig` ligger i `lokal`.
Leverandøren i `lokal` brukes med `NAIS_CLUSTER_NAME=lokal` (som Compose setter) eller når variabelen mangler.
Lokal kode er ikke med i prod- eller utviklingsdistribusjonen.
Produksjonsimaget kan derfor verken starte lokalt eller i dev-gcp, og utviklingsimaget starter bare i dev-gcp.
Diagnostikkruten `GET /sak/{saksnummer}/oppdragstilstand` finnes bare i utviklingsimaget, i tester og i `LokalMain`.
`./gradlew installDist` bygger distribusjonene for alle tre modulene, under `build/install/`, `dev/build/install/` og `lokal/build/install/`.
Fra rota velger `./gradlew :run` produksjonsappen, `./gradlew :dev:run` appen for utviklingsimaget og `./gradlew :lokal:run` lokal kjøring.
`:dev:run` krever `NAIS_CLUSTER_NAME=dev-gcp` og miljøvariablene `DevConfig` leser.
`./gradlew check` verifiserer at utviklingsdistribusjonen har samme JAR-filer som produksjon og én egen JAR med inngangspunktet.
Kontrollen feiler hvis produksjon inneholder dev-pakken eller hvis JAR-fila fra `dev` overlapper med klasser og ressurser fra produksjon.
Distribusjonskontrollen feiler også hvis produksjons- eller utviklingsdistribusjonen inneholder lokalpakken, `db/lokal`, `logback.local.xml` eller rotas testfixtures-JAR.

## Synlighet

Synlighetsregelen for Kotlin gjelder alle modulene.
Standardvalgene er:

- `private` for hjelpere som kun brukes innenfor en fil/klasse
- `public` (default) for deklarasjoner som er ment å brukes fra resten av modulen

Tester skal gå mot det offentlige inngangspunktet, ikke mot `private`-hjelpere – da unngår vi å låse oss til implementasjonsdetaljer.

---
# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne, tekniske henvendelser kan sendes via Slack i kanalen #tiltakspenger-værsågod.
