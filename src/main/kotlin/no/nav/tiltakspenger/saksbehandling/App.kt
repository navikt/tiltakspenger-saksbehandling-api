package no.nav.tiltakspenger.saksbehandling

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Bakgrunnsprosessoppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Jobboppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.prometheusMeterRegistry
import no.nav.tiltakspenger.libs.ktor.common.oppstart.startApp
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.setup.CALL_ID_MDC_KEY
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.ktorSetup
import java.time.Clock

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
 * Komposisjonsroten.
 * Her konstrueres registeret alle appens målinger føres i, og som `/metrics` skraper.
 * Registeret lages av `prometheusMeterRegistry()` fra libs, som binder det til Prometheus sitt globale register; se KDoc-en der.
 * Bindingen er nødvendig her fordi [no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister] registrerer tellerne sine rett på `PrometheusRegistry.defaultRegistry`, og de skal fortsatt bli med i skrapingen.
 * Testkontekstene og den lokale konteksten lager sitt eget register, fordi et prosessnavn bare kan registreres én gang per register.
 */
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
