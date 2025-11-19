package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import java.time.LocalDate

class TiltaksdeltakelseService(
    private val sakService: SakService,
    private val personService: PersonService,
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseKlient,
) {
    suspend fun hentTiltaksdeltakelserForSak(
        sakId: SakId,
        fraOgMed: LocalDate?,
        tilOgMed: LocalDate?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltaksdeltakelser, List<TiltaksdeltakelseMedArrangørnavn>> {
        if (fraOgMed == null || tilOgMed == null) {
            return KunneIkkeHenteTiltaksdeltakelser.OppslagsperiodeMangler.left()
        }

        if (!fraOgMed.isAfter(tilOgMed)) {
            return KunneIkkeHenteTiltaksdeltakelser.NegativOppslagsperiode.left()
        }

        val oppslagsperiode = Periode(fraOgMed, tilOgMed)
        val sak = sakService.hentForSakId(sakId)

        val person = personService.hentEnkelPersonFnr(sak.fnr)
            .getOrElse { return KunneIkkeHenteTiltaksdeltakelser.FeilVedKallMotPdl.left() }
        val harAdressebeskyttelse = person.fortrolig || person.strengtFortrolig || person.strengtFortroligUtland

        val alleTiltaksdeltakelser = tiltaksdeltakelseKlient.hentTiltaksdeltakelserMedArrangørnavn(
            fnr = sak.fnr,
            harAdressebeskyttelse = harAdressebeskyttelse,
            correlationId = correlationId,
        )

        // Viser bare tiltaksdeltakelser som overlapper med perioden, tiltak med manglende fom og tom vises ikke.
        return alleTiltaksdeltakelser.filter {
            it.periode?.overlapperMed(oppslagsperiode) == true
        }.right()
    }
}
