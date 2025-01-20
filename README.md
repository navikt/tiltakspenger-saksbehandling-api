tiltakspenger-saksbehandling-api
================

Håndterer vedtak om [tiltakspenger](https://www.nav.no/no/person/arbeid/oppfolging-og-tiltak-for-a-komme-i-jobb/stonader-ved-tiltak). Backend for [tiltakspenger-saksbehandling](https://github.com/navikt/tiltakspenger-saksbehandling).

En del av satsningen ["Flere i arbeid – P4"](https://memu.no/artikler/stor-satsing-skal-fornye-navs-utdaterte-it-losninger-og-digitale-verktoy/)


# Komme i gang
## Forutsetninger
- [JDK](https://jdk.java.net/)
- [Kotlin](https://kotlinlang.org/)
- [Gradle](https://gradle.org/) brukes som byggeverktøy og er inkludert i oppsettet


## Lokal kjøring

Appen kan kjøres opp lokalt med `main`-funksjonen i ([LokalMain.kt](https://github.com/navikt/tiltakspenger-saksbehandling-api/blob/main/app/src/test/kotlin/no/nav/tiltakspenger/LokalMain.kt)). Denne bruker mocks for de fleste tjenester, og krever kun en lokal database kjørende i tillegg.
  
Databasen kan startes med docker-compose i [meta-repoet for tiltakspenger](https://github.com/navikt/tiltakspenger). Se README der for fremgangsmåte og import av data.


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
# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne, tekniske henvendelser kan sendes via Slack i kanalen #tiltakspenger-værsågod.
