package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereBehandling

sealed interface KunneIkkeOppdatereBarnetillegg {
    data class KunneIkkeOppdatereBehandling(
        val valideringsfeil: KanIkkeOppdatereBehandling,
    ) : KunneIkkeOppdatereBarnetillegg
}
