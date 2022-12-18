package no.nav.tiltakspenger.vedtak.routes

import com.auth0.jwk.UrlJwkProvider
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.http.content.defaultResource
import io.ktor.server.http.content.resource
import io.ktor.server.http.content.static
import io.ktor.server.http.content.staticBasePackage
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.Rolle
import no.nav.tiltakspenger.vedtak.AdRolle
import no.nav.tiltakspenger.vedtak.Configuration
import no.nav.tiltakspenger.vedtak.InnsendingMediator
import no.nav.tiltakspenger.vedtak.SøkerMediator
import no.nav.tiltakspenger.vedtak.routes.rivers.skjermingRoutes
import no.nav.tiltakspenger.vedtak.routes.rivers.søknadRoutes
import no.nav.tiltakspenger.vedtak.routes.rivers.personopplysningerRoutes
import no.nav.tiltakspenger.vedtak.routes.rivers.tiltakRoutes
import no.nav.tiltakspenger.vedtak.routes.rivers.ytelseRoutes
import no.nav.tiltakspenger.vedtak.routes.saksbehandler.saksbehandlerRoutes
import no.nav.tiltakspenger.vedtak.routes.søker.søkerRoutes
import no.nav.tiltakspenger.vedtak.service.søker.SøkerService
import no.nav.tiltakspenger.vedtak.tilgang.JWTInnloggetSaksbehandlerProvider
import java.net.URI
import java.util.*

private val LOG = KotlinLogging.logger {}
private val SECURELOG = KotlinLogging.logger("tjenestekall")

internal fun Application.vedtakApi(
    config: Configuration.TokenVerificationConfig,
    innloggetSaksbehandlerProvider: JWTInnloggetSaksbehandlerProvider,
    søkerService: SøkerService,
    innsendingMediator: InnsendingMediator,
    søkerMediator: SøkerMediator,
) {
    install(CallId)
    install(CallLogging) {
        callIdMdc("call-id")
        disableDefaultColors()
        filter { call ->
            !call.request.path().startsWith("/isalive") &&
                    !call.request.path().startsWith("/isready") &&
                    !call.request.path().startsWith("/metrics")
        }
    }
    jacksonSerialization()
    auth(config)
    routing {
        authenticate("saksbehandling") {
            søkerRoutes(innloggetSaksbehandlerProvider, søkerService)
            saksbehandlerRoutes(innloggetSaksbehandlerProvider)
        }
        søknadRoutes(innsendingMediator, søkerMediator)
        skjermingRoutes(innsendingMediator)
        tiltakRoutes(innsendingMediator)
        ytelseRoutes(innsendingMediator)
        personopplysningerRoutes(innsendingMediator = innsendingMediator, søkerMediator = søkerMediator)
        static("/") {
            staticBasePackage = "static"
            resource("index.html")
            defaultResource("index.html")
        }
    }
}

fun Application.auth(config: Configuration.TokenVerificationConfig) {

    /*
    val jwkProvider: JwkProvider = JwkProviderBuilder(config.jwksUri)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
     */

    val jwkProviderGammel = UrlJwkProvider(URI(config.jwksUri).toURL())

    install(Authentication) {
        fun AuthenticationConfig.jwt(name: String, realm: String, roles: List<Rolle>? = null) =
            jwt(name) {
                SECURELOG.info { "config : $config" }
                this.realm = realm
                verifier(jwkProviderGammel, config.issuer) {
                    LOG.info { "Er nå i verifier" }
                    withAudience(config.clientId)
                    acceptLeeway(config.leeway)
                }
                challenge { _, _ ->
                    LOG.info { "verifier feilet" }
                    call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang")
                }
                validate { cred ->
                    SECURELOG.info("Cred er $cred")
                    LOG.info { "er nå i validate, skal ha preferred_username" }
                    if (cred.getClaim("preferred_username", String::class) == null) {
                        LOG.info { "Fant ikke preferred_username" }
                        return@validate null
                    }
                    LOG.info { "er nå i validate, skal ha NAVident" }
                    if (cred.getClaim("NAVident", String::class) == null) {
                        LOG.info { "Fant ikke NAVident" }
                        return@validate null
                    }

                    val claimedRoles: List<UUID> = cred.getListClaim("groups", UUID::class)
                    val configRoles: List<AdRolle> = config.roles
                    val authorizedRoles = configRoles
                        .filter { roles?.contains(it.name) ?: true }
                        .map { it.objectId }
                    if (claimedRoles.none(authorizedRoles::contains)) {
                        LOG.info { "Fant ikke riktig rolle" }
                        return@validate null
                    }

                    JWTPrincipal(cred.payload)
                }
            }
        jwt("saksbehandling", "saksbehandling", listOf(Rolle.SAKSBEHANDLER))
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
