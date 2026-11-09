package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgbarPeriode

data class InnvilgbarPeriodeDTO(
    val internDeltakelseId: String,
    val periode: PeriodeDTO,
)

fun List<InnvilgbarPeriode>.tilDTO(): List<InnvilgbarPeriodeDTO> {
    return this.map {
        InnvilgbarPeriodeDTO(
            internDeltakelseId = it.internDeltakelseId.toString(),
            periode = it.periode.toDTO(),
        )
    }
}
