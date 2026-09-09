package no.nav.tiltakspenger.saksbehandling.infra.setup

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Readiness
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.TexasAuthenticationProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.infra.route.routes

const val CALL_ID_MDC_KEY = "call-id"

fun Application.ktorSetup(
    applicationContext: ApplicationContext,
    readiness: Readiness,
    devRoutes: Route.(applicationContext: ApplicationContext) -> Unit = {},
) {
    install(CallId)
    install(CallLogging) {
        callIdMdc(CALL_ID_MDC_KEY)
        disableDefaultColors()
        filter { call ->
            !call.request.path().startsWith("/isalive") &&
                !call.request.path().startsWith("/isready") &&
                !call.request.path().startsWith("/metrics")
        }
    }
    metrics(applicationContext.meterRegistry)
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    configureExceptions()
    setupAuthentication(applicationContext.texasClient)
    routing { routes(applicationContext, readiness = readiness, devRoutes = devRoutes) }
}

/**
 * Kobler Ktor-metrikkene og `/metrics` til registeret appen allerede eier.
 * Registeret kommer inn som parameter i stedet for å konstrueres her, slik at jobbene og Kafka-consumerne fører målingene sine i nøyaktig det registeret som skrapes.
 * Konstruksjonen hører hjemme i komposisjonsroten, se `prometheusMeterRegistry()` i `App.kt`.
 */
fun Application.metrics(meterRegistry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        registry = meterRegistry
    }
    routing {
        get("/metrics") {
            call.respondText(
                text = meterRegistry.scrape(),
                status = HttpStatusCode.OK,
            )
        }
    }
}

fun Application.configureExceptions() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            ExceptionHandler.handle(call, cause)
        }
    }
}

fun Application.setupAuthentication(texasClient: TexasClient) {
    authentication {
        register(
            TexasAuthenticationProvider(
                TexasAuthenticationProvider.Config(
                    name = IdentityProvider.AZUREAD.value,
                    texasClient = texasClient,
                    identityProvider = IdentityProvider.AZUREAD,
                ),
            ),
        )
    }
}
