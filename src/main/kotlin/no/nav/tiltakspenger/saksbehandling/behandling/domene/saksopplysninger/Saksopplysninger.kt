package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import java.time.LocalDate

/**
 * Et sett med opplysninger som er relevante for saksbehandlingen.
 *
 * En [no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling] vil ha en referanse til [Saksopplysninger].
 *
 * @param periode perioden det er hentet saksopplysninger for. Bør henge sammen med tiltaksdeltagelsesperioden. Er null dersom tiltaksdeltagelsesperioden er null.
 */
data class Saksopplysninger(
    val fødselsdato: LocalDate,
    val tiltaksdeltagelser: Tiltaksdeltagelser,
    val periode: Periode?,
    val ytelser: Ytelser,
) {
    init {
        // Vi venter med å aktivere denne til etter vi har tømt dev-basen, som skjer ca. 27 august 2025.
//        require(periode == tiltaksdeltagelser.totalPeriode) {
//            "Periode $periode må være lik tiltaksdeltagelse sin totalPeriode ${tiltaksdeltagelser.totalPeriode}. Denne kan feile i en overgangsfase i dev-basen."
//        }
    }

    fun getTiltaksdeltagelse(eksternDeltagelseId: String): Tiltaksdeltagelse? {
        return tiltaksdeltagelser.getTiltaksdeltagelse(eksternDeltagelseId)
    }

    /**
     * Siden denne kun brukes av den del-automatiske behandlingen ønsker vi å behandle tvilstilfellene (null) som om de kan overlappe.
     */
    fun harOverlappendeTiltaksdeltakelse(eksternDeltakelseId: String, tiltaksperiode: Periode): Boolean {
        return tiltaksdeltagelser.any {
            it.eksternDeltagelseId != eksternDeltakelseId && (it.overlapperMedPeriode(tiltaksperiode) ?: true)
        }
    }

    fun harAndreYtelser(): Boolean = ytelser.isNotEmpty()
}

typealias HentSaksopplysninger = suspend (
    fnr: Fnr,
    correlationId: CorrelationId,
    tiltaksdeltagelserDetErSøktTiltakspengerFor: TiltaksdeltagelserDetErSøktTiltakspengerFor,
    aktuelleTiltaksdeltagelserForBehandlingen: List<String>,
    inkluderOverlappendeTiltaksdeltagelserDetErSøktOm: Boolean,
) -> Saksopplysninger
