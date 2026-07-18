package no.nav.tiltakspenger.saksbehandling

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Bakgrunnsprosessoppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.startApp
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.setup.CALL_ID_MDC_KEY
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.ktorSetup
import java.time.Clock

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile)

    val log = KotlinLogging.logger {}

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    log.info { "starting server" }
    start(log = log, clock = Clock.system(zoneIdOslo))
}

internal fun start(
    log: KLogger,
    port: Int = Configuration.httpPort,
    isNais: Boolean = Configuration.isNais(),
    clock: Clock,
    applicationContext: ApplicationContext = ApplicationContext(
        gitHash = Configuration.gitHash(),
        clock = clock,
    ),
    devRoutes: Route.(applicationContext: ApplicationContext) -> Unit = {},
) {
    log.info { "App context initialized" }

    startApp(
        log = log,
        port = port,
        isNais = isNais,
        oppsett = Bakgrunnsprosessoppsett(
            mdcCallIdKey = CALL_ID_MDC_KEY,
            electorPath = Configuration::electorPath,
            tasks = jobber(isNais = isNais, applicationContext = applicationContext, clock = clock),
            kafkaConsumers = kafkaConsumers(isNais = isNais, applicationContext = applicationContext),
            clock = applicationContext.clock,
        ),
    ) { readiness ->
        ktorSetup(applicationContext = applicationContext, readiness = readiness, devRoutes = devRoutes)
    }
}
