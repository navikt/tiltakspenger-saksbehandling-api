package no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.infra.route

import arrow.core.Either
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.saksbehandling.lokal.infra.route.DEV_ROUTE
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.KunneIkkeLagreLokalTiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.LagretLokalTiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.LokalTiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.LokalTiltaksdeltakelseService
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.OpprettEllerOppdaterLokalTiltaksdeltakelseCommand
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toDbJson
import java.time.LocalDate

private const val PATH = "$DEV_ROUTE/tiltaksdeltakelse"

private data class OpprettEllerOppdaterTiltaksdeltakelseBody(
    val eksternDeltakelseId: String?,
    val sakId: String?,
    val tiltakskode: String?,
    val typeNavn: String?,
    val kilde: Tiltakskilde?,
    val fraOgMed: LocalDate?,
    val tilOgMed: LocalDate?,
    val status: TiltakDeltakerstatus?,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
) {
    val eksternId: String? = eksternDeltakelseId?.trim()?.ifBlank { null }

    fun toCommand(sakId: SakId?) = OpprettEllerOppdaterLokalTiltaksdeltakelseCommand(
        eksternDeltakelseId = eksternId,
        sakId = sakId,
        tiltakskode = tiltakskode?.trim()?.ifBlank { null },
        typeNavn = typeNavn?.trim()?.ifBlank { null },
        kilde = kilde,
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        status = status,
        deltakelseProsent = deltakelseProsent,
        antallDagerPerUke = antallDagerPerUke,
    )
}

private data class LagretTiltaksdeltakelseRespons(
    val eksternDeltakelseId: String,
    val opprettet: Boolean,
    val girRett: Boolean,
    val deltakelse: Any,
)

private fun LagretLokalTiltaksdeltakelse.toRespons() = LagretTiltaksdeltakelseRespons(
    eksternDeltakelseId = deltakelse.eksternDeltakelseId,
    opprettet = opprettet,
    girRett = deltakelse is LokalTiltaksdeltakelse.GirRett,
    deltakelse = when (deltakelse) {
        is LokalTiltaksdeltakelse.GirRett -> deltakelse.deltakelse.toDbJson()
        is LokalTiltaksdeltakelse.GirIkkeRett -> deltakelse.deltakelse.toDbJson()
    },
)

/**
 * Oppretter eller setter tiltaksdeltakelsen som tiltakshistorikken svarer med lokalt.
 * Enten `eksternDeltakelseId` eller `sakId` må være satt.
 * En eksisterende deltakelse oppdateres, og det registreres en endring på deltakeren slik Kafka-consumerne gjør.
 * Felter som utelates beholder verdien fra deltakelsen som gjelder i dag.
 * Finnes ingen deltakelse med `eksternDeltakelseId`, opprettes en ny på `sakId` med `tiltakskode` (påkrevd), og ekstern id genereres dersom den mangler.
 * `tiltakskode` er navnet på en tiltakstype som gir rett (f.eks. `GRUPPE_AMO`) eller kildens kode for et lønnstilskudd (f.eks. `MIDLONTIL`).
 */
fun Route.oppdaterTiltaksdeltakelseDevRoute(lokalTiltaksdeltakelseService: LokalTiltaksdeltakelseService) {
    post(PATH) {
        call.withBody<OpprettEllerOppdaterTiltaksdeltakelseBody> { body ->
            val sakId = body.sakId?.trim()?.ifBlank { null }?.let {
                Either.catch { SakId.fromString(it) }.getOrNull()
                    ?: return@withBody call.respondText("Ugyldig sakId: $it", status = HttpStatusCode.BadRequest)
            }
            if (body.eksternId == null && sakId == null) {
                return@withBody call.respondText(
                    "Enten eksternDeltakelseId eller sakId må være satt",
                    status = HttpStatusCode.BadRequest,
                )
            }
            val command = body.toCommand(sakId)
            lokalTiltaksdeltakelseService.opprettEllerOppdater(command).fold(
                ifLeft = {
                    val (status, melding) = when (it) {
                        KunneIkkeLagreLokalTiltaksdeltakelse.FantIkkeDeltakelse ->
                            HttpStatusCode.NotFound to "Fant ingen tiltaksdeltakelse med ekstern id ${command.eksternDeltakelseId}. Oppgi sakId for å opprette en ny."

                        KunneIkkeLagreLokalTiltaksdeltakelse.FantIkkeSak ->
                            HttpStatusCode.NotFound to "Fant ingen sak med id ${command.sakId}"

                        KunneIkkeLagreLokalTiltaksdeltakelse.TilhørerAnnenSak ->
                            HttpStatusCode.BadRequest to "Tiltaksdeltakelsen ${command.eksternDeltakelseId} tilhører en annen sak enn ${command.sakId}"

                        KunneIkkeLagreLokalTiltaksdeltakelse.ManglerTiltakstype ->
                            HttpStatusCode.BadRequest to "tiltakskode er påkrevd når en ny tiltaksdeltakelse opprettes"

                        is KunneIkkeLagreLokalTiltaksdeltakelse.UkjentTiltakstype ->
                            HttpStatusCode.BadRequest to "${it.tiltakskode} er verken en tiltakstype som gir rett eller et lønnstilskudd"

                        is KunneIkkeLagreLokalTiltaksdeltakelse.KanIkkeEndres ->
                            HttpStatusCode.BadRequest to "${it.felt} kan ikke endres på en eksisterende tiltaksdeltakelse"

                        is KunneIkkeLagreLokalTiltaksdeltakelse.UgyldigDeltakelse ->
                            HttpStatusCode.BadRequest to it.melding
                    }
                    call.respondText(melding, status = status)
                },
                ifRight = {
                    call.respondJson(
                        value = it.toRespons(),
                        status = if (it.opprettet) HttpStatusCode.Created else HttpStatusCode.OK,
                    )
                },
            )
        }
    }
}
