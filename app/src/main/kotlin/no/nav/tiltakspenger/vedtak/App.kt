package no.nav.tiltakspenger.vedtak

import arrow.core.Either
import arrow.core.right
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.Route
import io.ktor.util.AttributeKey
import mu.KLogger
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookup
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupClient
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupFeil
import no.nav.tiltakspenger.libs.jobber.RunCheckFactory
import no.nav.tiltakspenger.vedtak.Configuration.applicationProfile
import no.nav.tiltakspenger.vedtak.Configuration.httpPort
import no.nav.tiltakspenger.vedtak.context.ApplicationContext
import no.nav.tiltakspenger.vedtak.jobber.TaskExecutor
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
    applicationContext: ApplicationContext = ApplicationContext(
        gitHash = Configuration.gitHash(),
    ),
    devRoutes: Route.(applicationContext: ApplicationContext) -> Unit = {},
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { e }
        sikkerlogg.error(e) { e.message }
    }

    val applicationProfile = applicationProfile()

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

    val stoppableTasks = TaskExecutor.startJob(
        initialDelay = if (isNais) 1.minutes else 1.seconds,
        runCheckFactory = runCheckFactory,
        tasks = listOf {
            applicationContext.utbetalingContext.sendUtbetalingerService.send()
            applicationContext.utbetalingContext.journalførUtbetalingsvedtakService.journalfør()
            applicationContext.behandlingContext.journalførVedtaksbrevService.journalfør()
            applicationContext.behandlingContext.distribuerVedtaksbrevService.distribuer()
            applicationContext.sendTilDatadelingService.send(Configuration.isNais())
            applicationContext.meldekortContext.oppgaveMeldekortService.opprettOppgaveForMeldekortSomIkkeGodkjennesAutomatisk()
            if (Configuration.isNais()) {
                applicationContext.endretTiltaksdeltakerJobb.opprettOppgaveForEndredeDeltakere()
                applicationContext.endretTiltaksdeltakerJobb.opprydning()
            }

            if (applicationProfile == Profile.DEV) {
                applicationContext.meldekortContext.sendMeldeperiodeTilBrukerService.send()
            }
        },
    )

    if (Configuration.isNais()) {
        val consumers = listOf(
            applicationContext.tiltaksdeltakerArenaConsumer,
            applicationContext.tiltaksdeltakerKometConsumer,
        )
        consumers.forEach { it.run() }
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.application.attributes.put(isReadyKey, false)
            server.stop(gracePeriodMillis = 5_000, timeoutMillis = 30_000)
        },
    )
    server.start(wait = true)
}

val isReadyKey = AttributeKey<Boolean>("isReady")

fun Application.isReady() = attributes.getOrNull(isReadyKey) == true
