package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse

data class Innvilgelsesperioder(
    val periodisering: IkkeTomPeriodisering<Innvilgelsesperiode>,
) {
    init {
    }
}

data class Innvilgelsesperiode(
    val periode: Periode,
    val valgtTiltaksdeltakelse: Tiltaksdeltakelse,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
) {

    init {
        require(valgtTiltaksdeltakelse.deltakelseFraOgMed != null && valgtTiltaksdeltakelse.deltakelseTilOgMed != null) {
            "Kan ikke velge tiltaksdeltakelse med id ${valgtTiltaksdeltakelse.eksternDeltakelseId} som mangler start- eller sluttdato"
        }

        val deltakelsesperiode =
            Periode(valgtTiltaksdeltakelse.deltakelseFraOgMed, valgtTiltaksdeltakelse.deltakelseTilOgMed)

        require(periode == deltakelsesperiode) {
            "Valgt deltakelsesperiode $deltakelsesperiode for tiltak med id ${valgtTiltaksdeltakelse.eksternDeltakelseId} må være lik innvilgelsesperioden $periode"
        }
    }
}
