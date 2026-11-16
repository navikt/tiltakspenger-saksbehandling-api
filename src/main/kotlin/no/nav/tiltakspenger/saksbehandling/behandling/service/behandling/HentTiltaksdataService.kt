package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdata
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.loggFeil
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.synligGjennomføringsnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.synligTittel

/**
 * Slår opp opplysninger om tiltakene i behandlingens saksopplysninger som vi ikke lagrer, som tittelen og gjennomføringsnavnet.
 * Tiltakene hentes fra den lagrede behandlingen, så klienten trenger ikke å vite hvilke tiltak det gjelder.
 */
class HentTiltaksdataService(
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val personService: PersonService,
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseKlient,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun hent(
        sakId: SakId,
        behandlingId: RammebehandlingId,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltaksdata, List<Tiltaksdata>> {
        val behandling = rammebehandlingRepo.hent(behandlingId)
        require(behandling.sakId == sakId) { "Behandling $behandlingId tilhører ikke sak $sakId" }
        val girRett = behandling.saksopplysninger.tiltaksdeltakelser
        val fratarRett = behandling.saksopplysninger.tiltaksdeltakelserSomFratarRett
        if (girRett.isEmpty() && fratarRett.isEmpty()) {
            return emptyList<Tiltaksdata>().right()
        }

        val harAdressebeskyttelse = personService.hentEnkelPersonFnr(behandling.fnr)
            .getOrElse { return KunneIkkeHenteTiltaksdata.FeilVedKallMotPdl.left() }
            .adressebeskyttelse.erGradert

        val fraKilden = tiltaksdeltakelseKlient.hentAlleTiltaksdeltakelser(behandling.fnr, correlationId)
            .getOrElse { feil ->
                feil.loggFeil(logger, "henting av tiltaksdata fra tiltakshistorikk", "sakId: $sakId, behandlingId: $behandlingId, correlationId: $correlationId")
                return KunneIkkeHenteTiltaksdata.FeilVedKallMotTiltak.left()
            }
            .associateBy { it.id.verdi }

        fun tiltaksdata(eksternDeltakelseId: String, internDeltakelseId: TiltaksdeltakerId?): Tiltaksdata {
            val deltakelse = fraKilden[eksternDeltakelseId]
            return Tiltaksdata(
                eksternDeltakelseId = eksternDeltakelseId,
                internDeltakelseId = internDeltakelseId,
                tittel = deltakelse?.synligTittel(harAdressebeskyttelse),
                gjennomføringsnavn = deltakelse?.synligGjennomføringsnavn(harAdressebeskyttelse),
            )
        }

        return (
            girRett.map { tiltaksdata(it.eksternDeltakelseId, it.internDeltakelseId) } +
                fratarRett.map { tiltaksdata(it.eksternDeltakelseId, null) }
            ).right()
    }
}

sealed interface KunneIkkeHenteTiltaksdata {
    data object FeilVedKallMotPdl : KunneIkkeHenteTiltaksdata

    data object FeilVedKallMotTiltak : KunneIkkeHenteTiltaksdata
}
