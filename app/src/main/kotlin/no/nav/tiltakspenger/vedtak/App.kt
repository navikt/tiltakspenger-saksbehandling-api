package no.nav.tiltakspenger.vedtak

import arrow.core.Either
import arrow.core.right
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KLogger
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookup
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupClient
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupFeil
import no.nav.tiltakspenger.libs.jobber.RunCheckFactory
import no.nav.tiltakspenger.vedtak.Configuration.applicationProfile
import no.nav.tiltakspenger.vedtak.Configuration.httpPort
import no.nav.tiltakspenger.vedtak.Configuration.isProd
import no.nav.tiltakspenger.vedtak.context.ApplicationContext
import no.nav.tiltakspenger.vedtak.jobber.TaskExecutor
import no.nav.tiltakspenger.vedtak.routes.vedtakApi
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
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { e }
        sikkerlogg.error(e) { e.message }
    }

    val runCheckFactory = if (isNais) {
        RunCheckFactory(
            leaderPodLookup =
            LeaderPodLookupClient(
                electorPath = Configuration.electorPath(),
                logger = KotlinLogging.logger { },
            ),
        )
    } else {
        RunCheckFactory(
            leaderPodLookup =
            object : LeaderPodLookup {
                override fun amITheLeader(localHostName: String): Either<LeaderPodLookupFeil, Boolean> =
                    true.right()
            },
        )
    }

    val stoppableTasks = TaskExecutor.startJob(
        initialDelay = if (isNais) 1.minutes else 1.seconds,
        runCheckFactory = runCheckFactory,
        tasks =
        listOf {
            // Kun dev: 22 januar var det datalast i OS/UR. Nå har helved og tømt basen sin. Vi stopper utbetaling frem til a) OS/UR har tømt sin base og helved har tømt sin igjen for tiltakspenger. b) vi tømmer devbasen vår c) vi slår på utbetalingsjobben igjen.
            if (isProd()) {
                applicationContext.utbetalingContext.sendUtbetalingerService.send()
            }
            applicationContext.utbetalingContext.journalførUtbetalingsvedtakService.journalfør()
            applicationContext.behandlingContext.journalførVedtaksbrevService.journalfør()
            applicationContext.behandlingContext.distribuerVedtaksbrevService.distribuer()
            applicationContext.sendTilDatadelingService.send(Configuration.isNais())

            if (applicationProfile() != Profile.PROD) {
                applicationContext.meldekortContext.sendMeldeperiodeTilBrukerService.send()
            }
        },
    )

    embeddedServer(
        factory = Netty,
        port = port,
        module = {
            vedtakApi(
                applicationContext = applicationContext,
            )
        },
    ).start(wait = true)
}
