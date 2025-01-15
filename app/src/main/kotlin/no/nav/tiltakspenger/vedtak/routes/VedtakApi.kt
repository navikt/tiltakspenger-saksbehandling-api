package no.nav.tiltakspenger.vedtak.routes

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.vedtak.context.ApplicationContext
import no.nav.tiltakspenger.vedtak.routes.behandling.behandlingBeslutterRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.behandlingRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.benk.behandlingBenkRoutes
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.ExceptionHandler
import no.nav.tiltakspenger.vedtak.routes.meldekort.meldekortRoutes
import no.nav.tiltakspenger.vedtak.routes.sak.sakRoutes
import no.nav.tiltakspenger.vedtak.routes.saksbehandler.saksbehandlerRoutes
import no.nav.tiltakspenger.vedtak.routes.søknad.søknadRoutes

const val CALL_ID_MDC_KEY = "call-id"

internal fun Application.vedtakApi(
    applicationContext: ApplicationContext,
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
    jacksonSerialization()
    configureExceptions()
    routing {
        healthRoutes()
        saksbehandlerRoutes(applicationContext.tokenService)
        behandlingRoutes(
            behandlingService = applicationContext.behandlingContext.behandlingService,
            tiltaksdeltagelseVilkårService = applicationContext.behandlingContext.tiltaksdeltagelseVilkårService,
            sakService = applicationContext.sakContext.sakService,
            kvpVilkårService = applicationContext.behandlingContext.kvpVilkårService,
            livsoppholdVilkårService = applicationContext.behandlingContext.livsoppholdVilkårService,
            auditService = applicationContext.personContext.auditService,
            tokenService = applicationContext.tokenService,
        )
        behandlingBenkRoutes(
            tokenService = applicationContext.tokenService,
            behandlingService = applicationContext.behandlingContext.behandlingService,
            sakService = applicationContext.sakContext.sakService,
            auditService = applicationContext.personContext.auditService,
            startRevurderingService = applicationContext.behandlingContext.startRevurderingService,
        )
        behandlingBeslutterRoutes(
            tokenService = applicationContext.tokenService,
            behandlingService = applicationContext.behandlingContext.behandlingService,
            auditService = applicationContext.personContext.auditService,
        )
        sakRoutes(
            tokenService = applicationContext.tokenService,
            sakService = applicationContext.sakContext.sakService,
            auditService = applicationContext.personContext.auditService,
        )
        meldekortRoutes(
            iverksettMeldekortService = applicationContext.meldekortContext.iverksettMeldekortService,
            sendMeldekortTilBeslutterService = applicationContext.meldekortContext.sendMeldekortTilBeslutterService,
            opprettMeldekortBehandlingService = applicationContext.meldekortContext.opprettMeldekortBehandlingService,
            auditService = applicationContext.personContext.auditService,
            sakService = applicationContext.sakContext.sakService,
            tokenService = applicationContext.tokenService,
        )
        søknadRoutes(applicationContext.søknadContext.søknadService, tokenService = applicationContext.tokenService)
        staticResources(
            remotePath = "/",
            basePackage = "static",
            index = "index.html",
            block = {
                default("index.html")
            },
        )
    }
}

fun Application.jacksonSerialization() {
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
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
