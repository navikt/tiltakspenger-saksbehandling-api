package no.nav.tiltakspenger.saksbehandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.context.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.repository.felles.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.routes.søknad.nySakMedNySøknad
import no.nav.tiltakspenger.saksbehandling.routes.søknad.nySøknadForFnr
import no.nav.tiltakspenger.saksbehandling.routes.withBody

internal const val DEV_ROUTE = "/dev"

internal fun Route.localDevRoutes(applicationContext: ApplicationContext) {
    data class NySøknadBody(
        val fnr: String?,
        val deltakelsesperiode: PeriodeDbJson?,
    )

    post("$DEV_ROUTE/soknad/ny") {
        call.withBody<NySøknadBody> {
            val fnr = it.fnr?.let { Fnr.tryFromString(it) }
            if (fnr == null) {
                val saksnummer = nySakMedNySøknad(
                    deltakelsesperiode = it.deltakelsesperiode?.toDomain(),
                    applicationContext = applicationContext,
                )
                call.respond(HttpStatusCode.OK, saksnummer.toString())
            } else {
                val saksnummer = nySøknadForFnr(
                    fnr = fnr,
                    applicationContext = applicationContext,
                    deltakelsesperiode = it.deltakelsesperiode?.toDomain(),
                )
                call.respond(HttpStatusCode.OK, saksnummer.toString())
            }
        }
    }
}
