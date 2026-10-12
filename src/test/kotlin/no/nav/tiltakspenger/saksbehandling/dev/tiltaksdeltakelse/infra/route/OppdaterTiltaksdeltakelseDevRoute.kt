package no.nav.tiltakspenger.saksbehandling.dev.tiltaksdeltakelse.infra.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.saksbehandling.dev.infra.route.DEV_ROUTE
import no.nav.tiltakspenger.saksbehandling.dev.tiltaksdeltakelse.KunneIkkeOppdatereLokalTiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.dev.tiltaksdeltakelse.LokalTiltaksdeltakelseService
import no.nav.tiltakspenger.saksbehandling.dev.tiltaksdeltakelse.OppdaterLokalTiltaksdeltakelseCommand
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toDbJson
import java.time.LocalDate

private const val PATH = "$DEV_ROUTE/tiltaksdeltakelse"

private data class OppdaterTiltaksdeltakelseBody(
    val eksternDeltakelseId: String,
    val fraOgMed: LocalDate?,
    val tilOgMed: LocalDate?,
    val status: TiltakDeltakerstatus?,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
) {
    fun toCommand() = OppdaterLokalTiltaksdeltakelseCommand(
        eksternDeltakelseId = eksternDeltakelseId,
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        status = status,
        deltakelseProsent = deltakelseProsent,
        antallDagerPerUke = antallDagerPerUke,
    )
}

/**
 * Setter tiltaksdeltakelsen som tiltakshistorikken svarer med lokalt, og registrerer en endring på deltakeren slik Kafka-consumerne gjør.
 * Felter som utelates beholder verdien fra deltakelsen som gjelder i dag.
 */
fun Route.oppdaterTiltaksdeltakelseDevRoute(lokalTiltaksdeltakelseService: LokalTiltaksdeltakelseService) {
    post(PATH) {
        call.withBody<OppdaterTiltaksdeltakelseBody> { body ->
            lokalTiltaksdeltakelseService.oppdater(body.toCommand()).fold(
                ifLeft = {
                    when (it) {
                        KunneIkkeOppdatereLokalTiltaksdeltakelse.FantIkkeDeltaker -> call.respondText(
                            "Fant ingen tiltaksdeltaker med ekstern id ${body.eksternDeltakelseId}",
                            status = HttpStatusCode.NotFound,
                        )

                        KunneIkkeOppdatereLokalTiltaksdeltakelse.FantIkkeDeltakelse -> call.respondText(
                            "Fant ingen tiltaksdeltakelse med ekstern id ${body.eksternDeltakelseId} for personen",
                            status = HttpStatusCode.NotFound,
                        )

                        is KunneIkkeOppdatereLokalTiltaksdeltakelse.UgyldigDeltakelse -> call.respondText(
                            it.melding,
                            status = HttpStatusCode.BadRequest,
                        )
                    }
                },
                ifRight = { call.respondJson(value = it.toDbJson()) },
            )
        }
    }
}
