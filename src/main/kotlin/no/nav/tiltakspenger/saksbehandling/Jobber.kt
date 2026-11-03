package no.nav.tiltakspenger.saksbehandling

import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.jobber.Kjøremodus
import no.nav.tiltakspenger.libs.jobber.TaskGruppe
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Miljøverdi
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import java.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

fun jobber(
    isNais: Boolean,
    applicationContext: ApplicationContext,
    clock: Clock,
): List<Task> = buildList {
    addAll(søknadsbehandlingJobber(applicationContext))
    addAll(utbetalingJobber(applicationContext))
    addAll(rammevedtaksbrevJobber(applicationContext))
    addAll(journalnotatJobber(applicationContext))
    addAll(klageJobber(applicationContext))
    addAll(meldekortJobber(applicationContext, clock))
    addAll(tiltaksdeltakerJobber(applicationContext))
    add(
        Task(
            navn = "saksbehandling-jobb-synkroniser-statistikk-kodeverk-tiltakstype",
            utfør = { _ ->
                if (applicationContext.statistikkContext.statistikkService.synkroniserTiltakstypekodeverk()) TaskResultat.Ferdig else TaskResultat.IngenArbeid
            },
        ),
    )

    if (isNais) {
        addAll(naisJobber(applicationContext))
    }
}

private fun søknadsbehandlingJobber(
    applicationContext: ApplicationContext,
): List<Task> = listOf(
    Task(
        navn = "saksbehandling-jobb-opprett-søknadsbehandling-fra-søknad",
        utfør = { _ ->
            applicationContext.delautomatiskSøknadsbehandlingJobb.opprettSøknadsbehandlingerFraNyeSøknader()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-behandle-søknader",
        utfør = { _ ->
            applicationContext.delautomatiskSøknadsbehandlingJobb.automatiskBehandleSøknadsbehandlinger()
            TaskResultat.Ferdig
        },
    ),
)

private fun utbetalingJobber(
    applicationContext: ApplicationContext,
): List<Task> = listOf(
    Task(
        navn = "saksbehandling-jobb-journalfør-meldekortvedtak",
        utfør = { _ ->
            applicationContext.utbetalingContext.journalførMeldekortvedtakService.journalfør()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-send-utbetalinger",
        utfør = { _ ->
            applicationContext.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-oppdater-utbetalingsstatus",
        utfør = { _ ->
            applicationContext.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()
            TaskResultat.Ferdig
        },
    ),
)

/**
 * Egen parallell gruppe, slik at pausen mellom oppslagene ikke holder igjen de serielle jobbene.
 * Gruppen tømmer køen med ett oppslag om gangen og venter 500 ms etter hvert, så sokos-utbetaldata får tid til andre konsumenter.
 * Prod har prøvedrift med et tak på antall oppslag; se maksAntallOppslag i UtbetalingContext.
 */
fun taskGrupper(
    applicationContext: ApplicationContext,
): List<TaskGruppe> = listOf(
    TaskGruppe(
        navn = "saksbehandling-jobb-oppdater-utbetalingsoversikt",
        intervall = 1.minutes,
        tasks = nonEmptyListOf({ _ -> applicationContext.utbetalingContext.oppdaterUtbetalingsoversiktJobb.oppdaterUtbetalingsoversikter() }),
        kjøremodus = Kjøremodus.PARALLELT,
        kjørKontinuerligTilTom = true,
        pauseMellomDrenering = 500.milliseconds,
    ),
)

private fun rammevedtaksbrevJobber(
    applicationContext: ApplicationContext,
): List<Task> = listOf(
    Task(
        navn = "saksbehandling-jobb-journalfør-rammevedtaksbrev",
        utfør = { _ ->
            applicationContext.behandlingContext.journalførRammevedtaksbrevService.journalfør()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-distribuer-rammevedtaksbrev",
        utfør = { _ ->
            applicationContext.behandlingContext.distribuerRammevedtaksbrevService.distribuer()
            TaskResultat.Ferdig
        },
    ),
)

private fun journalnotatJobber(
    applicationContext: ApplicationContext,
): List<Task> = listOf(
    Task(
        navn = "saksbehandling-jobb-journalfør-notater",
        utfør = { _ ->
            applicationContext.journalførJournalnotatService.journalførNotater()
            TaskResultat.Ferdig
        },
    ),
)

private fun klageJobber(
    applicationContext: ApplicationContext,
): List<Task> = listOf(
    Task(
        navn = "saksbehandling-jobb-journalfør-klagebrev-avvisning",
        utfør = { _ ->
            applicationContext.klagebehandlingContext.journalførKlagebrevJobb.journalførAvvisningbrev()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-journalfør-klagebrev-innstilling",
        utfør = { _ ->
            applicationContext.klagebehandlingContext.journalførKlagebrevJobb.journalførInnstillingsbrev()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-distribuer-klagebrev-avvisning",
        utfør = { _ ->
            applicationContext.klagebehandlingContext.distribuerKlagebrevJobb.distribuerAvvisningsbrev()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-distribuer-klagebrev-innstilling",
        utfør = { _ ->
            applicationContext.klagebehandlingContext.distribuerKlagebrevJobb.distribuerInnstillingsbrev()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-journalfør-klagebrev-forvaltningsmelding",
        utfør = { _ ->
            applicationContext.klagebehandlingContext.journalførKlagebrevJobb.journalførForvaltningsmeldinger()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-distribuer-klagebrev-forvaltningsmelding",
        utfør = { _ ->
            applicationContext.klagebehandlingContext.distribuerKlagebrevJobb.distribuerForvaltningsmeldinger()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-oversend-klage",
        utfør = { _ ->
            applicationContext.klagebehandlingContext.oversendKlageTilKlageinstansJobb.oversendKlagerTilKlageinstans()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-knytt-klagehendelse",
        utfør = { _ ->
            applicationContext.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelser()
            TaskResultat.Ferdig
        },
    ),
)

private fun meldekortJobber(
    applicationContext: ApplicationContext,
    clock: Clock,
): List<Task> = listOf(
    Task(
        navn = "saksbehandling-jobb-send-til-meldekort-api",
        utfør = { _ ->
            applicationContext.meldekortContext.sendTilMeldekortApiService.sendSaker()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-automatisk-meldekortbehandling",
        utfør = { _ ->
            applicationContext.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock)
            TaskResultat.Ferdig
        },
    ),
)

private fun tiltaksdeltakerJobber(
    applicationContext: ApplicationContext,
): List<Task> = listOf(
    Task(
        navn = "saksbehandling-jobb-endret-tiltaksdeltaker",
        utfør = { _ ->
            applicationContext.oppdatertTiltaksdeltakelseJobb.håndterUbehandledeEndringer()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-behandle-automatiske-revurderinger",
        utfør = { _ ->
            applicationContext.delautomatiskRevurderingJobb.automatiskBehandleRevurderinger()
            TaskResultat.Ferdig
        },
    ),
)

private fun naisJobber(
    applicationContext: ApplicationContext,
): List<Task> = listOf(
    Task(
        navn = "saksbehandling-jobb-send-til-datadeling",
        intervall = Miljøverdi.lik(1.minutes),
        initialDelay = Miljøverdi.lik(1.minutes),
        utfør = { _ ->
            applicationContext.sendTilDatadelingService.send()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-send-søknadsutfall-til-søknad-api",
        intervall = Miljøverdi.lik(1.minutes),
        initialDelay = Miljøverdi.lik(1.minutes),
        utfør = { _ ->
            applicationContext.sendSøknadsutfallTilSøknadApiJobb.send()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-personhendelse-opprett-opgave",
        intervall = Miljøverdi.lik(1.minutes),
        initialDelay = Miljøverdi.lik(1.minutes),
        utfør = { _ ->
            applicationContext.personhendelseJobb.opprettOppgaveForPersonhendelser()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-personhendelse-opprydning",
        intervall = Miljøverdi.lik(1.minutes),
        initialDelay = Miljøverdi.lik(1.minutes),
        utfør = { _ ->
            applicationContext.personhendelseJobb.opprydning()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-identhendelse",
        intervall = Miljøverdi.lik(1.minutes),
        initialDelay = Miljøverdi.lik(1.minutes),
        utfør = { _ ->
            applicationContext.identhendelseJobb.behandleIdenthendelser()
            TaskResultat.Ferdig
        },
    ),
    Task(
        navn = "saksbehandling-jobb-tilbakekreving-hendelser",
        intervall = Miljøverdi.lik(1.minutes),
        initialDelay = Miljøverdi.lik(1.minutes),
        utfør = { _ ->
            applicationContext.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()
            TaskResultat.Ferdig
        },
    ),
)
