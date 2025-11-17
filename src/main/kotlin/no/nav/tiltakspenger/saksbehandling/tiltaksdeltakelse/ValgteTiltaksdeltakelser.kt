package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser

/**
 * Saksbehandler skal kunne velge hvilke tiltaksdeltakelser man velger å innvilge tiltakspenger for i gitte perioder,
 * f.eks. ved overlappende tiltaksdeltakelser
 */
data class ValgteTiltaksdeltakelser(
    val periodisering: SammenhengendePeriodisering<Tiltaksdeltakelse>,
) {
    val verdier = periodisering.verdier

    fun krympPeriode(periode: Periode): ValgteTiltaksdeltakelser {
        return ValgteTiltaksdeltakelser(
            periodisering.krymp(periode) as SammenhengendePeriodisering<Tiltaksdeltakelse>,
        )
    }

    companion object {
        fun periodiser(
            tiltaksdeltakelser: List<Pair<Periode, String>>,
            behandling: Rammebehandling,
        ): ValgteTiltaksdeltakelser {
            return ValgteTiltaksdeltakelser(
                (tiltaksdeltakelser.tilPeriodisering() as SammenhengendePeriodisering).mapVerdi { verdi, periode ->
                    behandling.getTiltaksdeltakelse(verdi)
                        ?: throw IllegalArgumentException("Fant ikke tiltaksdeltakelse med eksternDeltakelseId $verdi i saksopplysningene.")
                },
            )
        }
    }

    init {
        periodisering.perioderMedVerdi.forEach {
            require(it.verdi.deltakelseFraOgMed != null && it.verdi.deltakelseTilOgMed != null) {
                "Kan ikke velge tiltaksdeltakelse med id ${it.verdi.eksternDeltakelseId} som mangler start- eller sluttdato"
            }
            val deltakelsesperiode = Periode(it.verdi.deltakelseFraOgMed!!, it.verdi.deltakelseTilOgMed!!)
            require(deltakelsesperiode.inneholderHele(it.periode)) {
                "Valgt periode ${it.periode} for tiltak med id ${it.verdi.eksternDeltakelseId} må være innenfor deltakelsesperioden $deltakelsesperiode"
            }
        }
    }

    fun getTiltaksdeltakelser(): Tiltaksdeltakelser {
        return this.periodisering.perioderMedVerdi.toList()
            .map { it.verdi }
            .distinctBy { it.eksternDeltakelseId }
            .let { Tiltaksdeltakelser(it) }
    }
}
