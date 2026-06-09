package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldeperiodebehandlingType

enum class MeldeperiodebehandlingTypeDTO {
    FØRSTE_BEHANDLING,
    KORRIGERING,
}

fun MeldeperiodebehandlingType.tilDTO(): MeldeperiodebehandlingTypeDTO = when (this) {
    MeldeperiodebehandlingType.FØRSTE_BEHANDLING -> MeldeperiodebehandlingTypeDTO.FØRSTE_BEHANDLING
    MeldeperiodebehandlingType.KORRIGERING -> MeldeperiodebehandlingTypeDTO.KORRIGERING
}
