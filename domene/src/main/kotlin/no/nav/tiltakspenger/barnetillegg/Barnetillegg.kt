package no.nav.tiltakspenger.barnetillegg

import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import java.time.LocalDate

/**
 * Representerer en periodisering av barnetillegg.
 */
data class Barnetillegg(
    val periodisering: Periodisering<AntallBarn>,
    val begrunnelse: BegrunnelseVilkårsvurdering?,
) {
    /** @return 0 dersom datoen er utenfor periodiseringen. */
    fun antallBarnPåDato(dato: LocalDate): AntallBarn {
        return periodisering.hentVerdiForDag(dato) ?: AntallBarn.ZERO
    }
}
