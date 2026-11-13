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
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.loggFeil
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.synligTittel

/**
 * Slår opp opplysninger om tiltakene i behandlingens saksopplysninger som vi ikke lagrer, som tittelen på tiltaket.
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

        val titler = tiltaksdeltakelseKlient.hentAlleTiltaksdeltakelser(behandling.fnr, correlationId)
            .getOrElse { feil ->
                feil.loggFeil(logger, "henting av tiltaksdata fra tiltakshistorikk", "sakId: $sakId, behandlingId: $behandlingId, correlationId: $correlationId")
                return KunneIkkeHenteTiltaksdata.FeilVedKallMotTiltak.left()
            }
            .associate { it.id.verdi to it.synligTittel(harAdressebeskyttelse) }

        return (
            girRett.map { Tiltaksdata(it.eksternDeltakelseId, it.internDeltakelseId, titler[it.eksternDeltakelseId]) } +
                fratarRett.map { Tiltaksdata(it.eksternDeltakelseId, null, titler[it.eksternDeltakelseId]) }
            ).right()
    }
}

sealed interface KunneIkkeHenteTiltaksdata {
    data object FeilVedKallMotPdl : KunneIkkeHenteTiltaksdata

    data object FeilVedKallMotTiltak : KunneIkkeHenteTiltaksdata
}
