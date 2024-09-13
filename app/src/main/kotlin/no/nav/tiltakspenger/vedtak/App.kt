package no.nav.tiltakspenger.vedtak

import arrow.core.Either
import arrow.core.right
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookup
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupClient
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupFeil
import no.nav.tiltakspenger.libs.jobber.RunCheckFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.vedtak.Configuration.httpPort
import no.nav.tiltakspenger.vedtak.context.ApplicationContext
import no.nav.tiltakspenger.vedtak.db.DataSourceSetup
import no.nav.tiltakspenger.vedtak.jobber.TaskExecutor
import no.nav.tiltakspenger.vedtak.routes.vedtakApi
import no.nav.tiltakspenger.vedtak.tilgang.JWTInnloggetSaksbehandlerProvider

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    val securelog = KotlinLogging.logger("tjenestekall")
    log.info { "starting server" }
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { e }
        securelog.error(e) { e.message }
    }

    val dataSource = DataSourceSetup.createDatasource()
    val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)
    val applicationContext = ApplicationContext(sessionFactory, Configuration.gitHash())
    val config = Configuration.TokenVerificationConfig()
    val innloggetSaksbehandlerProvider = JWTInnloggetSaksbehandlerProvider()

    val runCheckFactory =
        if (Configuration.isNais()) {
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
                    override fun amITheLeader(localHostName: String): Either<LeaderPodLookupFeil, Boolean> = true.right()
                },
            )
        }

    val stoppableTasks =
        TaskExecutor.startJob(
            runCheckFactory = runCheckFactory,
            tasks =
            listOf { correlationId ->
                applicationContext.utbetalingContext.opprettUtbetalingsvedtakService.opprettUtbetalingsvedtak()
                applicationContext.utbetalingContext.sendUtbetalingerService.send(correlationId)
                applicationContext.utbetalingContext.journalførUtbetalingsvedtakService.send(correlationId)
            },
        )

    embeddedServer(
        factory = Netty,
        port = httpPort(),
        module = {
            vedtakApi(
                config = config,
                innloggetSaksbehandlerProvider = innloggetSaksbehandlerProvider,
                applicationContext = applicationContext,
            )
        },
    ).start(wait = true)
}
