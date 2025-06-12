package no.nav.tiltakspenger.saksbehandling.ytelser.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.route.YtelseDTO

data class Ytelse(
    val ytelsetype: Ytelsetype,
    val perioder: List<Periode>,
) {
    fun toDTO() =
        YtelseDTO(
            ytelsetype = ytelsetype.tekstverdi,
            perioder = perioder.map { it.toDTO() },
        )
}
