package no.nav.tiltakspenger.saksbehandling.ytelser.infra.route

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO

data class YtelseDTO(
    val ytelsetype: String,
    val perioder: List<PeriodeDTO>,
)
