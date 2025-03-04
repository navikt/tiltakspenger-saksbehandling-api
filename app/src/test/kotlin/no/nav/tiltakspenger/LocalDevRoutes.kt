package no.nav.tiltakspenger

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.vedtak.context.ApplicationContext
import no.nav.tiltakspenger.vedtak.routes.søknad.nySøknadPåEksisterendeSak
import no.nav.tiltakspenger.vedtak.routes.withBody

internal const val DEV_ROUTE = "/dev"

val localClient = HttpClient(CIO) {
    defaultRequest {
        url("http://localhost:8080")
    }
}

internal fun Route.localDevRoutes(applicationContext: ApplicationContext) {
    val jwtGenerator = JwtGenerator()

    get("/dev/isAlive") {
        call.respondText("Hello, World!")
    }

    data class NySøknadBody(
        val fnr: String?,
    )

    post("$DEV_ROUTE/soknad/ny") {
        call.withBody<NySøknadBody> {
            val fnr = it.fnr?.let { Fnr.tryFromString(it) }
            if (fnr == null) {
                TODO()
            } else {
                nySøknadPåEksisterendeSak(
                    fnr = fnr,
                    client = localClient,
                    jwtGenerator = jwtGenerator,
                )
            }
        }
    }
}
