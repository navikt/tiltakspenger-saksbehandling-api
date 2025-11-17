package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Et sett med opplysninger som er relevante for saksbehandlingen.
 *
 * En [no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling] vil ha en referanse til [Saksopplysninger].
 *
 * @param periode perioden det er hentet saksopplysninger for. Bør henge sammen med tiltaksdeltagelsesperioden. Er null dersom tiltaksdeltagelsesperioden er null.
 */
data class Saksopplysninger(
    val fødselsdato: LocalDate,
    val tiltaksdeltakelser: Tiltaksdeltakelser,
    val ytelser: Ytelser,
    val tiltakspengevedtakFraArena: TiltakspengevedtakFraArena,
    val oppslagstidspunkt: LocalDateTime,
) {
    val periode: Periode? = tiltaksdeltakelser.totalPeriode

    fun kanInnvilges(tiltaksdeltagelseId: String): Boolean {
        return tiltaksdeltakelser.getTiltaksdeltagelse(tiltaksdeltagelseId)?.kanInnvilges ?: false
    }

    fun getTiltaksdeltakelse(eksternDeltagelseId: String): Tiltaksdeltakelse? {
        return tiltaksdeltakelser.getTiltaksdeltagelse(eksternDeltagelseId)
    }

    /**
     * Siden denne kun brukes av den del-automatiske behandlingen ønsker vi å behandle tvilstilfellene (null) som om de kan overlappe.
     */
    fun harOverlappendeTiltaksdeltakelse(eksternDeltakelseId: String, tiltaksperiode: Periode): Boolean {
        return tiltaksdeltakelser.any {
            it.eksternDeltagelseId != eksternDeltakelseId && (it.overlapperMedPeriode(tiltaksperiode) ?: true)
        }
    }

    fun harAndreYtelser(): Boolean = ytelser.isNotEmpty()

    fun harTiltakspengevedtakFraArena(): Boolean = tiltakspengevedtakFraArena.isNotEmpty()
}

typealias HentSaksopplysninger = suspend (
    fnr: Fnr,
    correlationId: CorrelationId,
    tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
    aktuelleTiltaksdeltagelserForBehandlingen: List<String>,
    inkluderOverlappendeTiltaksdeltagelserDetErSøktOm: Boolean,
) -> Saksopplysninger
