package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UtfyltMeldeperiode
import java.time.LocalDate

data class MeldekortDagDTO(
    val dato: LocalDate,
    val status: MeldekortDagStatusDTO,
)

fun UtfyltMeldeperiode.tilMeldekortDagerDTO(): List<MeldekortDagDTO> {
    return this.toList().map {
        MeldekortDagDTO(
            dato = it.dato,
            status = it.status.tilMeldekortDagStatusDTO(),
        )
    }
}
