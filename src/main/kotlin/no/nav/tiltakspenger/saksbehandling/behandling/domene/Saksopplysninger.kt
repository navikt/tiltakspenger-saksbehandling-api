package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import java.time.LocalDate

/**
 * Et sett med opplysninger som er relevante for saksbehandlingen.
 *
 * En [Behandling] vil ha en referanse til [Saksopplysninger].
 */
data class Saksopplysninger(
    val fødselsdato: LocalDate,
    val tiltaksdeltagelse: List<Tiltaksdeltagelse>,
    val periode: Periode,
    val ytelser: List<Ytelse>,
) {
    fun getTiltaksdeltagelse(eksternDeltagelseId: String): Tiltaksdeltagelse? =
        tiltaksdeltagelse.find { it.eksternDeltagelseId == eksternDeltagelseId }
}
