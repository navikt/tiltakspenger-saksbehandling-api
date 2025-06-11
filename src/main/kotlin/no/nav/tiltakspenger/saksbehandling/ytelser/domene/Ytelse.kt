package no.nav.tiltakspenger.saksbehandling.ytelser.domene

import no.nav.tiltakspenger.libs.periodisering.Periode

data class Ytelse(
    val ytelsetype: Ytelsetype,
    val perioder: List<Periode>,
)
