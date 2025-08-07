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
    val tiltaksdeltagelse: Tiltaksdeltagelser,
    val periode: Periode?,
    val ytelser: Ytelser,
) {
    init {
    }
    fun getTiltaksdeltagelse(eksternDeltagelseId: String): Tiltaksdeltagelse? =
        tiltaksdeltagelse.find { it.eksternDeltagelseId == eksternDeltagelseId }

    fun harOverlappendeTiltaksdeltakelse(eksternDeltakelseId: String, tiltaksperiode: Periode): Boolean =
        tiltaksdeltagelse.any {
            it.eksternDeltagelseId != eksternDeltakelseId &&
                it.overlapperMedPeriode(tiltaksperiode)
        }

    fun harAndreYtelser(): Boolean =
        ytelser.isNotEmpty()
}

typealias HentSaksopplysninger = suspend (fnr: Fnr, correlationId: CorrelationId, periode: Periode) -> Saksopplysninger
