package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling

/**
 * Saksbehandler skal kunne velge hvilke tiltaksdeltakelser man velger å innvilge tiltakspenger for i gitte perioder,
 * f.eks. ved overlappende tiltaksdeltakelser
 */
data class ValgteTiltaksdeltakelser(
    val periodisering: SammenhengendePeriodisering<Tiltaksdeltagelse>,
) {
    companion object {
        fun periodiser(
            tiltaksdeltakelser: List<Pair<Periode, String>>,
            behandling: Behandling,
        ): ValgteTiltaksdeltakelser {
            return ValgteTiltaksdeltakelser(
                (tiltaksdeltakelser.tilPeriodisering() as SammenhengendePeriodisering).mapVerdi { verdi, periode ->
                    behandling.getTiltaksdeltagelse(verdi)
                        ?: throw IllegalArgumentException("Fant ikke tiltaksdeltagelse med eksternDeltagelseId $verdi i saksopplysningene.")
                },
            )
        }
    }

    init {
        periodisering.perioderMedVerdi.forEach {
            require(it.verdi.deltagelseFraOgMed != null && it.verdi.deltagelseTilOgMed != null) {
                "Kan ikke velge tiltaksdeltakelse med id ${it.verdi.eksternDeltagelseId} som mangler start- eller sluttdato"
            }
            val deltagelsesperiode = Periode(it.verdi.deltagelseFraOgMed!!, it.verdi.deltagelseTilOgMed!!)
            require(deltagelsesperiode.inneholderHele(it.periode)) {
                "Valgt periode ${it.periode} for tiltak med id ${it.verdi.eksternDeltagelseId} må være innenfor deltakelsesperioden $deltagelsesperiode"
            }
        }
    }

    fun getTiltaksdeltakelser(): List<Tiltaksdeltagelse> {
        return this.periodisering.perioderMedVerdi.toList().map { it.verdi }.distinctBy { it.eksternDeltagelseId }
    }
}
