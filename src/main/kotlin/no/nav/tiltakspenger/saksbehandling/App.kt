package no.nav.tiltakspenger.saksbehandling

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.Route
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Bakgrunnsprosessoppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Jobboppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.startApp
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.setup.CALL_ID_MDC_KEY
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.ktorSetup
import java.time.Clock
import io.micrometer.core.instrument.Clock as MicrometerClock

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile)
    System.setProperty("org.apache.avro.SERIALIZABLE_PACKAGES", Configuration.avroSerializablePackages)

    val log = KotlinLogging.logger {}

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    log.info { "starting server" }
    start(log = log, clock = Clock.system(zoneIdOslo))
}

/**
 * Registeret appen eksponerer på `/metrics`, og som jobbene og Kafka-consumerne fører målingene sine i.
 * Det er bevisst bundet til Prometheus sitt globale register: [no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister] registrerer tellerne sine rett på [PrometheusRegistry.defaultRegistry], og de skal fortsatt bli med i skrapingen.
 * [MicrometerClock] er Micrometers egen klokke og har ingenting med appens [Clock] å gjøre; den brukes kun til å konstruere registeret.
 *
 * Funksjonen er privat slik at ingenting under `src/test` kan bruke prod-registeret, heller ikke den lokale konteksten `LokalMain` starter med.
 * Et globalt register er prosessglobal tilstand som ikke kan varieres per test, og et prosessnavn kan bare registreres én gang per register.
 * Testkontekstene og den lokale konteksten lager i stedet sitt eget `PrometheusMeterRegistry(PrometheusConfig.DEFAULT)`.
 */
private fun prometheusMeterRegistry(): PrometheusMeterRegistry = PrometheusMeterRegistry(
    PrometheusConfig.DEFAULT,
    PrometheusRegistry.defaultRegistry,
    MicrometerClock.SYSTEM,
)

fun start(
    log: KLogger,
    port: Int = Configuration.httpPort,
    host: String = "0.0.0.0",
    isNais: Boolean = Configuration.isNais(),
    clock: Clock,
    applicationContext: ApplicationContext = ApplicationContext(
        gitHash = Configuration.gitHash(),
        clock = clock,
        meterRegistry = prometheusMeterRegistry(),
        erDev = Configuration.isDev(),
    ),
    devRoutes: Route.(applicationContext: ApplicationContext) -> Unit = {},
) {
    log.info { "App context initialized" }

    startApp(
        log = log,
        port = port,
        host = host,
        isNais = isNais,
        oppsett = bakgrunnsprosessoppsett(applicationContext = applicationContext, isNais = isNais),
    ) { readiness ->
        ktorSetup(applicationContext = applicationContext, readiness = readiness, devRoutes = devRoutes)
    }
}

/**
 * Bakgrunnsprosessene appen kjører: de skedulerte jobbene fra [jobber] og Kafka-consumerne fra [kafkaConsumers].
 * Funksjonen ligger i komposisjonsroten fordi lista er komposisjonsrotens: det er her det avgjøres hva appen faktisk starter.
 * Wiring-testen kaller den for å starte de samme jobbene som produksjon, slik at målingene den sjekker er de ekte.
 */
fun bakgrunnsprosessoppsett(applicationContext: ApplicationContext, isNais: Boolean): Bakgrunnsprosessoppsett = Bakgrunnsprosessoppsett(
    jobber = Jobboppsett(
        mdcCallIdKey = CALL_ID_MDC_KEY,
        electorPath = Configuration::electorPath,
        clock = applicationContext.clock,
        meterRegistry = applicationContext.meterRegistry,
        tasks = jobber(isNais = isNais, applicationContext = applicationContext, clock = applicationContext.clock),
    ),
    kafkaConsumers = kafkaConsumers(isNais = isNais, applicationContext = applicationContext),
)
