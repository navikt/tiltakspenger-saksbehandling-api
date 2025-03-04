package no.nav.tiltakspenger

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.vedtak.context.ApplicationContext
import no.nav.tiltakspenger.vedtak.routes.søknad.nySøknadPåEksisterendeSak
import no.nav.tiltakspenger.vedtak.routes.withBody

internal const val DEV_ROUTE = "/dev"

internal fun Route.localDevRoutes(applicationContext: ApplicationContext) {
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
                    applicationContext = applicationContext,
                )
            }
            call.respond(HttpStatusCode.OK, "Ok")
        }
    }
}
