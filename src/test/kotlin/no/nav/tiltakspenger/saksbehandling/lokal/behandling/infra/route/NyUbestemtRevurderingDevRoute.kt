package no.nav.tiltakspenger.saksbehandling.lokal.behandling.infra.route

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.lokal.infra.route.DEV_ROUTE
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import java.time.LocalDate

private const val PATH = "$DEV_ROUTE/revurdering/ubestemt"

private enum class Endringstype {
    AVSLUTTET_SOM_FORVENTET,
    AVBRUTT_DELTAKELSE,
    IKKE_AKTUELL_DELTAKELSE,
    FORLENGELSE,
    AVKORTET,
    ANDRE_ENDRINGER,
}

/**
 * Feltene utover [sakId] og [endringstype] brukes bare av endringstypene som har dem.
 * For [Endringstype.ANDRE_ENDRINGER] blir hvert felt som er satt en egen endring, og minst ett må være satt.
 */
private data class NyUbestemtRevurderingBody(
    val sakId: String,
    val endringstype: Endringstype,
    val nySluttdato: LocalDate? = null,
    val nyStartdato: LocalDate? = null,
    val nyDeltakelsesprosent: Float? = null,
    val nyDagerPerUke: Float? = null,
    val nyStatus: TiltakDeltakerstatus? = null,
) {
    private val endretDeltakelsesmengde = if (nyDeltakelsesprosent != null || nyDagerPerUke != null) {
        TiltaksdeltakerEndring.EndretDeltakelsesmengde(nyDeltakelsesprosent, nyDagerPerUke)
    } else {
        null
    }

    fun tilEndring(): Either<String, TiltaksdeltakerEndring> = when (endringstype) {
        Endringstype.AVSLUTTET_SOM_FORVENTET -> TiltaksdeltakerEndring.AvsluttetSomForventet.right()

        Endringstype.AVBRUTT_DELTAKELSE -> TiltaksdeltakerEndring.AvbruttDeltakelse.right()

        Endringstype.IKKE_AKTUELL_DELTAKELSE -> TiltaksdeltakerEndring.IkkeAktuellDeltakelse.right()

        Endringstype.FORLENGELSE -> nySluttdato?.let {
            TiltaksdeltakerEndring.Forlengelse(nySluttdato = it, endretDeltakelsesmengde = endretDeltakelsesmengde).right()
        } ?: "Forlengelse krever ny sluttdato".left()

        Endringstype.AVKORTET -> nySluttdato?.let {
            TiltaksdeltakerEndring.Avkortet(nySluttdato = it).right()
        } ?: "Avkortet krever ny sluttdato".left()

        Endringstype.ANDRE_ENDRINGER -> if (nyStartdato == null && nySluttdato == null && endretDeltakelsesmengde == null && nyStatus == null) {
            "Andre endringer krever minst én endring".left()
        } else {
            TiltaksdeltakerEndring.AndreEndringer(
                endretDeltakelsesmengde = endretDeltakelsesmengde,
                endretStartdato = nyStartdato?.let { TiltaksdeltakerEndring.EndretStartdato(it) },
                endretSluttdato = nySluttdato?.let { TiltaksdeltakerEndring.EndretSluttdato(it) },
                endretStatus = nyStatus?.let { TiltaksdeltakerEndring.EndretStatus(it) },
            ).right()
        }
    }
}

private data class NyUbestemtRevurderingRespons(
    val saksnummer: String,
    val behandlingId: String,
)

/**
 * Oppretter en ubestemt revurdering slik systemet ville gjort det, siden den ikke kan opprettes av en saksbehandler.
 * Endringen knyttes til den første tiltaksdeltakelsen det er søkt om.
 */
fun Route.nyUbestemtRevurderingDevRoute(
    sakService: SakService,
    startRevurderingService: StartRevurderingService,
) {
    post(PATH) {
        call.withBody<NyUbestemtRevurderingBody> { body ->
            val endring = body.tilEndring().getOrElse {
                call.respond400BadRequest(melding = it, kode = "ugyldig_endring")
                return@withBody
            }
            val sakId = Either.catch { SakId.fromString(body.sakId.trim()) }.getOrElse {
                call.respond400BadRequest(melding = "Ugyldig sak-id", kode = "ugyldig_sak_id")
                return@withBody
            }
            val sak = sakService.hentForSakId(sakId)
            val tiltaksdeltakerId = sak.tiltaksdeltakelserDetErSøktTiltakspengerFor.firstOrNull()?.søknadstiltak?.tiltaksdeltakerId
            val (_, revurdering) = startRevurderingService.startRevurdering(
                kommando = StartRevurderingKommando.Ubestemt(
                    sakId = sak.id,
                    correlationId = CorrelationId.generate(),
                    automatiskOpprettetGrunn = AutomatiskOpprettetRevurderingGrunn(
                        endring = endring,
                        tiltaksdeltakerId = tiltaksdeltakerId,
                    ),
                ),
                sak = sak,
            ).getOrElse {
                call.respond400BadRequest(
                    melding = "Kunne ikke opprette ubestemt revurdering: $it",
                    kode = "kunne_ikke_opprette_ubestemt_revurdering",
                )
                return@withBody
            }
            call.respondJson(
                value = NyUbestemtRevurderingRespons(
                    saksnummer = sak.saksnummer.verdi,
                    behandlingId = revurdering.id.toString(),
                ),
            )
        }
    }
}
