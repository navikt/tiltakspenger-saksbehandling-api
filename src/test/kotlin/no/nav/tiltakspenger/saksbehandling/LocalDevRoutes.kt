package no.nav.tiltakspenger.saksbehandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.nySakMedNySøknad
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.nySøknadForFnr

internal const val DEV_ROUTE = "/dev"

internal fun Route.localDevRoutes(applicationContext: ApplicationContext) {
    data class NySøknadBody(
        val fnr: String?,
        val deltakelsesperiode: PeriodeDbJson?,
    )

    post("$DEV_ROUTE/soknad/ny") {
        call.withBody<NySøknadBody> { body ->
            val fnr = body.fnr?.let { Fnr.tryFromString(it) }
            if (fnr == null) {
                val saksnummer = nySakMedNySøknad(
                    deltakelsesperiode = body.deltakelsesperiode?.toDomain(),
                    applicationContext = applicationContext,
                )
                call.respond(HttpStatusCode.OK, saksnummer.toString())
            } else {
                val saksnummer = nySøknadForFnr(
                    fnr = fnr,
                    applicationContext = applicationContext,
                    deltakelsesperiode = body.deltakelsesperiode?.toDomain(),
                )
                call.respond(HttpStatusCode.OK, saksnummer.toString())
            }
        }
    }
}
