package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandling

/**
 * Saksbehandler skal kunne velge hvilke tiltaksdeltakelser man velger å innvilge tiltakspenger for i gitte perioder,
 * f.eks. ved overlappende tiltaksdeltakelser
 */
data class ValgteTiltaksdeltakelser(
    val periodisering: Periodisering<Tiltaksdeltagelse>,
) {
    companion object {
        fun periodiser(
            tiltaksdeltakelser: List<Pair<Periode, String>>,
            behandling: Behandling,
        ): ValgteTiltaksdeltakelser {
            val tiltaksdeltagelseMap = tiltaksdeltakelser.associate {
                it.second to (
                    behandling.getTiltaksdeltagelse(it.second)
                        ?: throw IllegalArgumentException("Kan ikke velge tiltaksdeltakelse med id ${it.second}")
                    )
            }
            val perioderMedVerdi = tiltaksdeltakelser.map { PeriodeMedVerdi(verdi = tiltaksdeltagelseMap[it.second]!!, periode = it.first) }
            return ValgteTiltaksdeltakelser(
                periodisering = Periodisering(perioderMedVerdi),
            )
        }
    }

    init {
        periodisering.perioderMedVerdi.forEach {
            if (it.verdi.deltagelseFraOgMed != null && it.verdi.deltagelseTilOgMed != null) {
                require(Periode(it.verdi.deltagelseFraOgMed!!, it.verdi.deltagelseTilOgMed!!).inneholderHele(it.periode)) {
                    "Valgt periode for tiltak med id ${it.verdi.eksternDeltagelseId} må være innenfor deltakelsesperioden"
                }
            } else if (it.verdi.deltagelseTilOgMed == null && it.verdi.deltagelseFraOgMed != null) {
                require(!it.periode.fraOgMed.isBefore(it.verdi.deltagelseFraOgMed)) {
                    "Valgt periode for tiltak med id ${it.verdi.eksternDeltagelseId} kan ikke starte før deltakelsesperioden"
                }
            } else if (it.verdi.deltagelseFraOgMed == null && it.verdi.deltagelseTilOgMed != null) {
                require(!it.periode.tilOgMed.isAfter(it.verdi.deltagelseTilOgMed)) {
                    "Valgt periode for tiltak med id ${it.verdi.eksternDeltagelseId} kan ikke slutte etter deltakelsesperioden"
                }
            }
        }
    }
}
