package no.nav.tiltakspenger.barnetillegg

import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import java.time.LocalDate

/**
 * Representerer en periodisering av barnetillegg.
 */
data class Barnetillegg(
    val value: Periodisering<Int>,
    val begrunnelse: BegrunnelseVilkårsvurdering?,
) {
    init {
        require(value.all { it.verdi in 0..99 }) { "Barnetillegg må være et tall mellom 0 og 99" }
    }

    /**
     * @return 0 dersom datoen er utenfor periodiseringen.
     */
    fun antallBarnPåDato(dato: LocalDate): Int {
        return value.hentVerdiForDag(dato) ?: 0
    }
}
