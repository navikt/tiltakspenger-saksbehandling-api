package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingType

enum class MeldekortbehandlingTypeDTO {
    FØRSTE_BEHANDLING,
    KORRIGERING,
}

fun MeldekortbehandlingType.tilDTO(): MeldekortbehandlingTypeDTO = when (this) {
    MeldekortbehandlingType.FØRSTE_BEHANDLING -> MeldekortbehandlingTypeDTO.FØRSTE_BEHANDLING
    MeldekortbehandlingType.KORRIGERING -> MeldekortbehandlingTypeDTO.KORRIGERING
}
