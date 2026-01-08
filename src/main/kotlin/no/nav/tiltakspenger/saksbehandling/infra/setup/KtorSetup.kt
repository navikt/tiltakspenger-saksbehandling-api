package no.nav.tiltakspenger.saksbehandling.infra.setup

import arrow.integrations.jackson.module.NonEmptyCollectionsModule
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
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
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.TexasAuthenticationProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import org.apache.kafka.shaded.com.google.protobuf.TextFormat
import java.text.Format

const val CALL_ID_MDC_KEY = "call-id"

internal fun Application.ktorSetup(
    applicationContext: ApplicationContext,
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
    metrics()
    // Kommentar jah: Denne skal egentlig ikke være i bruk, men legger den inn som et sikkerhetsnett frem til vi har migrert oss vekk fra call.respond( i common libs. Bør også gå over alt av implisitt retur av JSON, som f.eks. auth.
    //  Denne er uforandret. Bare kopiert fra funksjonen som var brukt av testene.
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            registerModule(JavaTimeModule())
            registerModule(NonEmptyCollectionsModule())
            registerModule(KotlinModule.Builder().build())
        }
    }
    configureExceptions()
    setupAuthentication(applicationContext.texasClient)
    routing { routes(applicationContext, devRoutes) }
}

fun Application.metrics() {
    val appMicrometerRegistry = PrometheusMeterRegistry(
        PrometheusConfig.DEFAULT,
        PrometheusRegistry.defaultRegistry,
        Clock.SYSTEM,
    )

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }
    routing {
        get("/metrics") {
            call.respondText(
                text = appMicrometerRegistry.scrape(),
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
