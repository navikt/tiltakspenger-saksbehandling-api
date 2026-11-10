# Oppfølging av utbetalinger

## Kilder og kontakt

| Område | Dokumentasjon | Kontrakt og kildekode |
| --- | --- | --- |
| V3-utbetaling på Kafka | [Opprett utbetaling](https://helved-docs.intern.dev.nav.no/kom_i_gang/opprett_utbetaling) | [AsyncAPI](https://github.com/navikt/helved-docs/blob/main/public/asyncapi-spec.yml), [betalingsmodell for tiltakspenger](https://github.com/navikt/helved-utbetaling/blob/main/models/main/models/TpDto.kt), [Kafka-mottaker](https://github.com/navikt/helved-utbetaling/blob/main/apps/abetal/main/abetal/Kafka.kt) |
| Status på Kafka | [Sjekk status på utbetaling](https://helved-docs.intern.dev.nav.no/kom_i_gang/sjekk_status_pa_utbetaling) | [Statusmodell](https://github.com/navikt/helved-utbetaling/blob/main/models/main/models/Status.kt), [statusproduksjon fra Oppdrag](https://github.com/navikt/helved-utbetaling/blob/main/apps/urskog/main/urskog/Kafka.kt) |
| Synkron V3-simulering | [Simuler en utbetaling](https://helved-docs.intern.dev.nav.no/kom_i_gang/simuler_en_utbetaling) | [HTTP-endepunktet](https://github.com/navikt/helved-utbetaling/blob/main/apps/simulering/main/simulering/DryrunRoutes.kt), [responsmodellene](https://github.com/navikt/helved-utbetaling/blob/main/models/main/models/Simulering.kt) |
| V2 over REST | [Iverksetting](https://helved-docs.intern.dev.nav.no/v2/doc/), [status](https://helved-docs.intern.dev.nav.no/v2/doc/status), [simulering](https://helved-docs.intern.dev.nav.no/v2/doc/simulering) | [Utsjekk](https://github.com/navikt/helved-utbetaling/tree/main/apps/utsjekk) |

Eierteamet nås i [#team-hel-ved](https://nav-it.slack.com/archives/C06SJTR2X3L) og finnes i [Teamkatalogen](https://teamkatalogen.nav.no/team/93e78ae9-babc-452c-965d-e7dc67af8612).
Se også vår [Kafka-producer](../src/main/kotlin/no/nav/tiltakspenger/saksbehandling/utbetaling/infra/kafka/UtbetalingKafkaProducer.kt), [statusconsumer](../src/main/kotlin/no/nav/tiltakspenger/saksbehandling/utbetaling/infra/kafka/HelvedStatusConsumer.kt) og [alarmregler](../.nais/alerts.yml).

AsyncAPI beskriver topic-navnet `tiltakspenger.utbetaling.v1` og UUID som meldingsnøkkel.
Integrasjonen vår bruker `tpts.utbetaling-v1` med `utbetaling_<ULID>` som nøkkel; topic-navnet samsvarer med [mottakeren til Hel Ved](https://github.com/navikt/helved-utbetaling/blob/main/apps/abetal/main/abetal/Kafka.kt).
Dokumentasjonen og den kjørende kontrakten må derfor ikke antas å være identiske på disse punktene.

## Oversikt

Versjon 2 sender utbetalinger og henter status over REST.
Versjon 3 sender utbetalinger på Kafka og lagrer relevante statusmeldinger rått i `utbetalingskvittering` før offset bekreftes.
Kafka-metadata hindrer at samme melding lagres flere ganger og bestemmer kørekkefølgen.
Kvitterings-ID-en er en ULID-identitet; Kafka-offset, ikke ULID eller mottakstidspunkt, avgjør rekkefølgen.
Versjon 3 bruker `utbetaling_<ULID>` som Kafka-nøkkel, slik at kvitteringen kan kobles direkte til utbetalingen.
For versjon 2 er Kafka-nøkkelen intern i Utsjekk; koblingen bruker i stedet `detaljer.linjer[].behandlingId`, som inneholder henvisningene vi sender.
Henvisningen er de siste 15 ULID-tegnene fra utbetalings-id-en.
Oppslaget vurderer alle referansene og krever en entydig kobling til utbetaling og sak; nøkkelens UUID-format brukes ikke til å avgjøre om kvitteringen gjelder V2.
En koblet V2-kvittering lagres uten å endre utbetalingsstatus.
V2 REST-sending, simulering og statuspolling er uendret.

## Sending på versjon 3

Versjon 3 lagrer den ferdige JSON-meldingen i en egen transaksjon før første Kafka-forsøk.
Første lagring vinner; senere forsøk gjenbruker den lagrede teksten og samme utbetalingsnøkkel.
Dersom sendingen lykkes og database-commit feiler, sendes nøyaktig samme melding ved neste forsøk, også om sakens ident er endret.
Dette garanterer likt meldingsinnhold, ikke at Kafka leverer meldingen bare én gang.
Feil ved lagring før sending hindrer Kafka-forsøket.
Versjon 2 er uendret.

## Synkron simulering

Sakens lagrede oppdragsversjon avgjør hvilken simuleringsklient som brukes, også før første utbetaling.
Versjon 3 bruker simuleringstjenestens `POST /api/simulering` med hele sakstilstanden og `dryrun = true`.
Tjenesten bruker Kafka internt, men returnerer resultatet i HTTP-svaret.
Hel Ved velger responsformatet `v1` for tiltakspenger; `info` med `OK_UTEN_ENDRING` betyr ingen endring.
Klienten har 130 sekunders timeout for å gi rom for tjenestens ventetid på inntil 120 sekunder.
Ukjente svar og feil blir ikke tolket som ingen endring, og det finnes ingen automatisk overgang til V2 ved feil.
Versjon 2 beholder eksisterende endepunkt og flyt.

## Kvitteringer og nye forsøk

`RåUtbetalingskvittering` representerer innboksmeldingen, med rå nøkkel og innhold, behandlingstilstand og eventuelle koblinger.
Mapperen i infra skiller mellom status og simulering, og bevarer referansene som trengs for oppslaget.
Etter kobling brukes den typede domenemodellen `Utbetalingskvittering`, uten rå Kafka-nøkkel eller JSON i statusbehandlingen.
Det er fortsatt én jobb; modellskillet gir separate steg for tolkning, kobling og behandling, ikke separate køer.

`BehandleUtbetalingskvitteringerJobb` behandler kvitteringer i numerisk offset-rekkefølge innen topic og partisjon.
En ubehandlet kvittering blokkerer høyere offset for samme nøkkel i samme topic og partisjon, også mens den venter på neste forsøk.
Ulike nøkler og null-nøkler blokkerer ikke hverandre.
Dette forutsetter at samme utbetalingsnøkkel konsekvent havner i samme Kafka-partisjon.
Kafka gir ingen rekkefølge på tvers av partisjoner; sorteringen mellom partisjoner i køen er deterministisk, ikke en global meldingsrekkefølge.
Statusen og ferdigmarkeringen lagres i samme transaksjon.
En sen `MOTTATT` eller `HOS_OPPDRAG` skal ikke overskrive en terminal utbetalingsstatus.
Simuleringer og kvitteringer for kjente utbetalinger på versjon 2 markeres som behandlet uten å endre utbetalingen.
Når Kafka-nøkkelen peker på en kjent utbetaling, kobles kvitteringen til saken før innholdet tolkes.
Koblingen til sak og utbetaling lagres også ved ugyldig innhold eller når den senere statusbehandlingen rulles tilbake og prøves på nytt.
Rå lagring fra konsumenten krever ingen sakskobling.

For referanseoppslag må alle referansene finnes og peke entydig på én utbetaling og sak.
Mangler koblingen, eller er treffene tvetydige, beholdes hendelsen for nye forsøk uten å velge en vilkårlig utbetaling.
En kjent sak kan bevares selv om utbetalingskoblingen ikke er entydig.
Disse koblingsfeilene inngår i varslingen etter tre dager; de skjules ikke ved å markere hendelsen ferdig.

Ved feil beholdes kvitteringen som ubehandlet med feiltype, klassifisering og `Forsøkshistorikk`.
Ventetidene er 1, 5, 15 og 30 minutter, deretter 1, 2, 4, 8, 12 og 24 timer.
Videre forsøk venter ett døgn hver gang.
Ventetiden overlever omstart.
Kvitteringer med feil blokkerer høyere offset for samme nøkkel i samme topic og partisjon, men ikke andre nøkler eller partisjoner.

Ugyldig nøkkel, manglende innhold og ugyldig meldingsformat krever at kode eller data rettes.
Manglende utbetaling og uventede tekniske feil behandles som forbigående feil.
Også permanente feil prøves på nytt etter backoff, slik at rettede data eller kode tas i bruk uten at meldingen må publiseres på nytt.
Et nytt behandlingsforsøk sender aldri utbetalingen på nytt til Hel Ved.

`FEILET` er en behandlet feilkvittering, ikke en teknisk feil i kvitteringsjobben.
Utbetalingen får `FeiletMotOppdrag` og krever manuell oppfølging.
En senere kvittering kan oppdatere statusen.
HTTP-feilkoden i meldingen brukes ikke som løfte om at Hel Ved prøver utbetalingen på nytt.

## Eksisterende alarmer

- **Utbetaling har feilet!** omfatter feilede utbetalinger og permanente feil i ubehandlede kvitteringer.
- **Utbetaling har ikke gått ok etter 3 dager!** omfatter utbetalinger uten OK tre dager etter opprettelse, også usendte utbetalinger og utbetalinger uten kvittering, samt forbigående kvitteringsfeil tre dager etter mottak.

For versjon 3 leses avvikene fra databasen ved hver jobbkjøring, også når køen er tom eller kvitteringene venter på neste forsøk.
De eksisterende alarmtellerne økes så lenge avvikene finnes; de teller observasjoner, ikke unike utbetalinger.
Dermed fortsetter varslingen etter omstart og under lang backoff, og stopper når avvikene er rettet og alarmens ti minutters vindu er utløpt.
Varslingen for versjon 2 er uendret.

Bruk `kvitteringId`, `utbetalingId`, `sakId` og `saksnummer` i loggene for å finne avviket.
Rå meldinger og feil som kan inneholde personopplysninger finnes bare i databasen og sikkerloggen.
Ved manglende kvittering må Hel Ved bekrefte utfallet før eventuell ny sending.
Rett årsaken før kvitteringen behandles på nytt; ikke merk en feil som ferdig bare for å dempe alarmen.
