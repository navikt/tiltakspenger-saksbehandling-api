package no.nav.tiltakspenger.saksbehandling.domene.tiltak

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering

/**
 * Saksbehandler skal kunne velge hvilke tiltaksdeltakelser man velger å innvilge tiltakspenger for i gitte perioder,
 * f.eks. ved overlappende tiltaksdeltakelser
 */
data class ValgteTiltaksdeltakelser(
    val periodisering: Periodisering<Tiltaksdeltagelse>,
) {
    companion object {
        // TODO: Sett inn det saksbehandler faktisk valgte
        fun periodiser(
            tiltaksdeltagelse: Tiltaksdeltagelse,
            periode: Periode,
        ): ValgteTiltaksdeltakelser {
            return ValgteTiltaksdeltakelser(
                periodisering = Periodisering(
                    PeriodeMedVerdi(tiltaksdeltagelse, periode),
                ),
            )
        }
    }
}
