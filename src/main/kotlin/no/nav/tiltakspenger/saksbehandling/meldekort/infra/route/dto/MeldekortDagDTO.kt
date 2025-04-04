package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDager
import java.time.LocalDate

data class MeldekortDagDTO(
    val dato: LocalDate,
    val status: MeldekortDagStatusMotFrontendDTO,
)

fun MeldekortDager.tilMeldekortDagerDTO(): List<MeldekortDagDTO> {
    return this.toList().map {
        MeldekortDagDTO(
            dato = it.dato,
            status = it.status.tilMeldekortDagStatusDTO(),
        )
    }
}
