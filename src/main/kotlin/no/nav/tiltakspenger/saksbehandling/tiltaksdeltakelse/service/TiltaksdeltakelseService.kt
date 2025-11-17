package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient

class TiltaksdeltakelseService(
    private val sakService: SakService,
    private val personService: PersonService,
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseKlient,
) {
    suspend fun hentTiltaksdeltakelserForSak(
        sakId: SakId,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltaksdeltakelser.FeilVedKallMotPdl, List<TiltaksdeltakelseMedArrangørnavn>> {
        val sak = sakService.hentForSakId(sakId)

        val person = personService.hentEnkelPersonFnr(sak.fnr)
            .getOrElse { return KunneIkkeHenteTiltaksdeltakelser.FeilVedKallMotPdl.left() }
        val harAdressebeskyttelse = person.fortrolig || person.strengtFortrolig || person.strengtFortroligUtland

        return tiltaksdeltakelseKlient.hentTiltaksdeltakelserMedArrangørnavn(
            fnr = sak.fnr,
            harAdressebeskyttelse = harAdressebeskyttelse,
            correlationId = correlationId,
        ).right()
    }
}
