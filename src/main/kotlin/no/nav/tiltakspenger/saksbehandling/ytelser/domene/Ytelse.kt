package no.nav.tiltakspenger.saksbehandling.ytelser.domene

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.route.YtelseDTO

data class Ytelse(
    val ytelsetype: Ytelsetype,
    val perioder: List<Periode>,
) {
    fun overlapperMed(periode: Periode): Boolean = perioder.any { it.overlapperMed(periode) }

    fun toDTO(): YtelseDTO {
        return YtelseDTO(
            ytelsetype = if (ytelsetype == Ytelsetype.UNGDOMSPROGRAMYTELSE) {
                "Ungdomsprogramytelse"
            } else {
                ytelsetype.tekstverdi
            },
            perioder = perioder.map { it.toDTO() },
        )
    }
}
