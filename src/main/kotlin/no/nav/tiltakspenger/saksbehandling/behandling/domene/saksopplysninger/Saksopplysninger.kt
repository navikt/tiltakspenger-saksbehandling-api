package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.periode.Overlapp
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomFratarRett
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Et sett med opplysninger som er relevante for saksbehandlingen.
 *
 * En [no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling] vil ha en referanse til [Saksopplysninger].
 *
 * @param periode perioden det er hentet saksopplysninger for.
 * Bør henge sammen med tiltaksdeltakelsesperioden.
 * Er null dersom tiltaksdeltakelsesperioden er null.
 * @param tiltaksdeltakelserSomFratarRett tiltaksdeltakelser som fratar rett til tiltakspenger, og som vises til saksbehandler (i første omgang lønnstilskudd).
 * Inneholder de som overlapper med [periode], inkludert tvilstilfellene der manglende datoer gjør at vi ikke kan si sikkert.
 * Brukes aldri som grunnlag for beregning, men sender søknaden til manuell behandling i den del-automatiske behandlingen.
 */
data class Saksopplysninger(
    val fødselsdato: LocalDate,
    val tiltaksdeltakelser: Tiltaksdeltakelser,
    val tiltaksdeltakelserSomFratarRett: List<TiltaksdeltakelseSomFratarRett>,
    val ytelser: Ytelser,
    val tiltakspengevedtakFraArena: TiltakspengevedtakFraArena,
    val oppslagstidspunkt: LocalDateTime,
) {
    val periode: Periode? = tiltaksdeltakelser.totalPeriode

    fun kanInnvilges(internDeltakelseId: TiltaksdeltakerId): Boolean {
        return tiltaksdeltakelser.getTiltaksdeltakelse(internDeltakelseId)?.kanInnvilges ?: false
    }

    fun getTiltaksdeltakelse(internDeltakelseId: TiltaksdeltakerId): TiltaksdeltakelseIntern? {
        return tiltaksdeltakelser.getTiltaksdeltakelse(internDeltakelseId)
    }

    /**
     * Siden denne kun brukes av den del-automatiske behandlingen ønsker vi å behandle tvilstilfellene (null) som om de kan overlappe.
     */
    fun harOverlappendeTiltaksdeltakelse(internDeltakelseId: TiltaksdeltakerId, tiltaksperiode: Periode): Boolean {
        return tiltaksdeltakelser.any {
            it.internDeltakelseId != internDeltakelseId && it.overlapperMed(tiltaksperiode) != Overlapp.Nei
        }
    }

    /**
     * Siden denne kun brukes av den del-automatiske behandlingen ønsker vi å behandle tvilstilfellene (null) som om de kan overlappe.
     */
    fun tiltaksdeltakelserSomFratarRettSomOverlapper(periode: Periode): List<TiltaksdeltakelseSomFratarRett> {
        return tiltaksdeltakelserSomFratarRett.filter { it.overlapperMed(periode) != Overlapp.Nei }
    }

    /**
     * Tiltakspenger fra utbetaldata regnes ikke som en annen ytelse.
     */
    fun harAndreYtelserEnnTiltakspengerSomOverlapper(periode: Periode): Boolean {
        return ytelser.any { it.ytelsetype != Ytelsetype.TILTAKSPENGER && it.overlapperMed(periode) }
    }

    /**
     * Siden denne kun brukes av den del-automatiske behandlingen ønsker vi å behandle vedtak uten sluttdato som om de kan overlappe.
     */
    fun harTiltakspengevedtakFraArenaSomOverlapper(periode: Periode): Boolean {
        return tiltakspengevedtakFraArena.any { it.overlapperMed(periode) != Overlapp.Nei }
    }
}

typealias HentSaksopplysninger = suspend (
    fnr: Fnr,
    correlationId: CorrelationId,
    tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
    aktuelleTiltaksdeltakelserForBehandlingen: List<TiltaksdeltakerId>,
    inkluderOverlappendeTiltaksdeltakelserDetErSøktOm: Boolean,
    sakId: SakId,
) -> Saksopplysninger
