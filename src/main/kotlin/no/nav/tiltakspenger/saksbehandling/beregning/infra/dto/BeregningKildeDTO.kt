package no.nav.tiltakspenger.saksbehandling.beregning.infra.dto

import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.BeregningKildeDTO.BeregningKildeTypeDTO

data class BeregningKildeDTO(
    val id: String,
    val type: BeregningKildeTypeDTO,
) {
    enum class BeregningKildeTypeDTO {
        MELDEKORT,
        RAMMEBEHANDLING,
    }
}

fun BeregningKilde.tilBeregningKildeDTO(): BeregningKildeDTO {
    return BeregningKildeDTO(
        id = this.id.toString(),
        type = when (this) {
            is BeregningKilde.BeregningKildeRammebehandling -> BeregningKildeTypeDTO.RAMMEBEHANDLING
            is BeregningKilde.BeregningKildeMeldekort -> BeregningKildeTypeDTO.MELDEKORT
        },
    )
}
