package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Et sett med opplysninger som er relevante for saksbehandlingen.
 *
 * En [no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling] vil ha en referanse til [Saksopplysninger].
 *
 * @param periode perioden det er hentet saksopplysninger for. Bør henge sammen med tiltaksdeltakelsesperioden. Er null dersom tiltaksdeltakelsesperioden er null.
 */
data class Saksopplysninger(
    val fødselsdato: LocalDate,
    val tiltaksdeltakelser: Tiltaksdeltakelser,
    val ytelser: Ytelser,
    val tiltakspengevedtakFraArena: TiltakspengevedtakFraArena,
    val oppslagstidspunkt: LocalDateTime,
) {
    val periode: Periode? = tiltaksdeltakelser.totalPeriode

    fun kanInnvilges(internDeltakelseId: TiltaksdeltakerId): Boolean {
        return tiltaksdeltakelser.getTiltaksdeltakelse(internDeltakelseId)?.kanInnvilges ?: false
    }

    fun getTiltaksdeltakelse(internDeltakelseId: TiltaksdeltakerId): Tiltaksdeltakelse? {
        return tiltaksdeltakelser.getTiltaksdeltakelse(internDeltakelseId)
    }

    /**
     * Siden denne kun brukes av den del-automatiske behandlingen ønsker vi å behandle tvilstilfellene (null) som om de kan overlappe.
     */
    fun harOverlappendeTiltaksdeltakelse(internDeltakelseId: TiltaksdeltakerId, tiltaksperiode: Periode): Boolean {
        return tiltaksdeltakelser.any {
            it.internDeltakelseId != internDeltakelseId && (it.overlapperMedPeriode(tiltaksperiode) ?: true)
        }
    }

    fun harAndreYtelserEnnTiltakspenger(): Boolean {
        if (ytelser.isNotEmpty()) {
            return ytelser.value.any { it.ytelsetype != Ytelsetype.TILTAKSPENGER }
        }
        return false
    }

    fun harTiltakspengevedtakFraArena(): Boolean = tiltakspengevedtakFraArena.isNotEmpty()
}

typealias HentSaksopplysninger = suspend (
    fnr: Fnr,
    correlationId: CorrelationId,
    tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
    aktuelleTiltaksdeltakelserForBehandlingen: List<String>,
    inkluderOverlappendeTiltaksdeltakelserDetErSøktOm: Boolean,
) -> Saksopplysninger
