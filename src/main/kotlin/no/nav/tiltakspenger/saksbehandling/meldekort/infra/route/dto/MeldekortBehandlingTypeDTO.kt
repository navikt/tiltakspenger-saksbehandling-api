package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType

enum class MeldekortBehandlingTypeDTO {
    FØRSTE_BEHANDLING,
    KORRIGERING,
}

fun MeldekortBehandlingType.tilDTO(): MeldekortBehandlingTypeDTO = when (this) {
    MeldekortBehandlingType.FØRSTE_BEHANDLING -> MeldekortBehandlingTypeDTO.FØRSTE_BEHANDLING
    MeldekortBehandlingType.KORRIGERING -> MeldekortBehandlingTypeDTO.KORRIGERING
}
