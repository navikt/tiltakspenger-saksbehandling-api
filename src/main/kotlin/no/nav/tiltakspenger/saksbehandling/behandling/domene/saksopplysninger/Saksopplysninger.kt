package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import java.time.LocalDate

/**
 * Et sett med opplysninger som er relevante for saksbehandlingen. En [no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling] vil ha en referanse til [Saksopplysninger].
 */
data class Saksopplysninger(
    val fødselsdato: LocalDate,
    val tiltaksdeltagelse: List<Tiltaksdeltagelse>,
) {
    fun getTiltaksdeltagelse(eksternDeltagelseId: String): Tiltaksdeltagelse? =
        tiltaksdeltagelse.find { it.eksternDeltagelseId == eksternDeltagelseId }
}
