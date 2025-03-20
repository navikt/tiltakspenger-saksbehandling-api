package no.nav.tiltakspenger.saksbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.Route

/**
 * Starter opp serveren lokalt med postgres og auth i docker og in-memory fakes.
 * Gjenbruker lokale innstillinger i [Configuration]
 * Gjenbruker fakes fra testene.
 * Dette er et alternativ til å starte opp serveren med docker-compose (som bruker wiremock for eksterne tjenester).
 */
fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    log.info { "Starter lokal server. Bruker default postgres i docker og in-memory fakes." }
    val localApplicationContext = LocalApplicationContext(usePdfGen = true)
    start(
        log = log,
        isNais = false,
        applicationContext = localApplicationContext,
        devRoutes = Route::localDevRoutes,
    )
}
