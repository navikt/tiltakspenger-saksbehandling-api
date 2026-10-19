package no.nav.tiltakspenger.saksbehandling.arenavedtak.domene

import no.nav.tiltakspenger.libs.periode.Overlapp
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.ÅpenPeriode
import java.time.LocalDate

data class ArenaTPVedtak(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val rettighet: Rettighet,
    val vedtakId: Long,
) {
    /**
     * Et vedtak uten sluttdato kan overlappe med alt etter at det startet, og gir [Overlapp.Kanskje].
     * Se [ÅpenPeriode.overlapperMed].
     */
    fun overlapperMed(periode: Periode): Overlapp = ÅpenPeriode(fraOgMed, tilOgMed).overlapperMed(periode)

    enum class Rettighet {
        TILTAKSPENGER,
        BARNETILLEGG,
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
    }
}
