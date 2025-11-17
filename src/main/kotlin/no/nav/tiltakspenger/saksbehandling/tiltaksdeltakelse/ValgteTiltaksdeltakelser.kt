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

    fun getTiltaksdeltakelser(): Tiltaksdeltakelser {
        return this.periodisering.perioderMedVerdi.toList()
            .map { it.verdi }
            .distinctBy { it.eksternDeltagelseId }
            .let { Tiltaksdeltakelser(it) }
    }
}
