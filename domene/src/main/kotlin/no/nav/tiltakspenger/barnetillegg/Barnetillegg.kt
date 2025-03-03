package no.nav.tiltakspenger.barnetillegg

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.inneholderOverlapp
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

    companion object {
        /**
         * Periodiserer og fyller ut hull med 0.
         * @throws IllegalArgumentException Dersom periodene er utenfor virkningsperioden eller overlapper.
         */
        fun periodiserOgFyllUtHullMed0(
            perioder: List<Pair<Periode, AntallBarn>>,
            begrunnelse: BegrunnelseVilkårsvurdering?,
            virkningsperiode: Periode,
        ): Barnetillegg {
            if (perioder.map { it.first }.inneholderOverlapp()) {
                throw IllegalArgumentException("Periodene kan ikke overlappe")
            }
            return Barnetillegg(
                periodisering = perioder.fold(
                    Periodisering(
                        AntallBarn.ZERO,
                        virkningsperiode,
                    ),
                ) { periodisering, (periode, antallBarn) ->
                    periodisering.setVerdiForDelPeriode(antallBarn, periode)
                },
                begrunnelse = begrunnelse,
            )
        }
    }
}
