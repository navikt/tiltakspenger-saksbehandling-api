package no.nav.tiltakspenger.saksbehandling

import arrow.core.Either
import arrow.core.right
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.Route
import io.ktor.util.AttributeKey
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookup
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupClient
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupFeil
import no.nav.tiltakspenger.libs.jobber.RunCheckFactory
import no.nav.tiltakspenger.libs.periodisering.zoneIdOslo
import no.nav.tiltakspenger.saksbehandling.infra.jobber.TaskExecutor
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration.httpPort
import no.nav.tiltakspenger.saksbehandling.infra.setup.ktorSetup
import java.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    log.info { "starting server" }
    start(log)
}

internal fun start(
    log: KLogger,
    port: Int = httpPort(),
    isNais: Boolean = Configuration.isNais(),
    clock: Clock = Clock.system(zoneIdOslo),
    applicationContext: ApplicationContext = ApplicationContext(
        gitHash = Configuration.gitHash(),
        clock = clock,
    ),
    devRoutes: Route.(applicationContext: ApplicationContext) -> Unit = {},
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    val server = embeddedServer(
        factory = Netty,
        port = port,
        module = { ktorSetup(applicationContext, devRoutes) },
    )
    server.application.attributes.put(isReadyKey, true)

    val runCheckFactory = if (isNais) {
        RunCheckFactory(
            leaderPodLookup =
            LeaderPodLookupClient(
                electorPath = Configuration.electorPath(),
                logger = KotlinLogging.logger { },
            ),
            applicationIsReady = { server.application.isReady() },
        )
    } else {
        RunCheckFactory(
            leaderPodLookup =
            object : LeaderPodLookup {
                override fun amITheLeader(localHostName: String): Either<LeaderPodLookupFeil, Boolean> =
                    true.right()
            },
            applicationIsReady = { server.application.isReady() },
        )
    }

    val jobber: TaskExecutor = TaskExecutor.startJob(
        initialDelay = if (isNais) 1.minutes else 1.seconds,
        runCheckFactory = runCheckFactory,
        tasks = listOf<suspend () -> Any>(
            { applicationContext.utbetalingContext.sendUtbetalingerService.send() },
            { applicationContext.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus() },
            { applicationContext.utbetalingContext.journalførUtbetalingsvedtakService.journalfør() },
            { applicationContext.behandlingContext.journalførVedtaksbrevService.journalfør() },
            { applicationContext.behandlingContext.distribuerVedtaksbrevService.distribuer() },
            { applicationContext.meldekortContext.oppgaveMeldekortService.opprettOppgaveForMeldekortSomIkkeBehandlesAutomatisk() },
            { applicationContext.genererMeldeperioderService.genererMeldeperioderForSaker() },
            { applicationContext.meldekortContext.sendTilMeldekortApiService.send() },
            { applicationContext.meldekortContext.automatiskMeldekortBehandlingService.behandleBrukersMeldekort() },
        ).let {
            if (Configuration.isNais()) {
                it.plus(
                    listOf(
                        { applicationContext.endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere() },
                        { applicationContext.endretTiltaksdeltakerJobb.opprydning() },
                        { applicationContext.sendTilDatadelingService.send() },
                        { applicationContext.personhendelseJobb.opprettOppgaveForPersonhendelser() },
                        { applicationContext.personhendelseJobb.opprydning() },
                        { applicationContext.identhendelseJobb.behandleIdenthendelser() },
                    ),
                )
            } else {
                it
            }
        },
    )

    if (Configuration.isNais()) {
        val consumers = listOf(
            applicationContext.tiltaksdeltakerArenaConsumer,
            applicationContext.tiltaksdeltakerKometConsumer,
            applicationContext.leesahConsumer,
            applicationContext.aktorV2Consumer,
        )
        consumers.forEach { it.run() }
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.application.attributes.put(isReadyKey, false)
            jobber.stop()
            server.stop(gracePeriodMillis = 5_000, timeoutMillis = 30_000)
        },
    )
    server.start(wait = true)
}

val isReadyKey = AttributeKey<Boolean>("isReady")

fun Application.isReady() = attributes.getOrNull(isReadyKey) == true
