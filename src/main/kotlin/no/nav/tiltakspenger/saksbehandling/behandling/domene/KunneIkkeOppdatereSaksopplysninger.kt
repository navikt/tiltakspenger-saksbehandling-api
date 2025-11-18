package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KunneIkkeOppdatereSaksopplysninger {
    data class KunneIkkeOppdatereBehandling(
        val valideringsfeil: KanIkkeOppdatereBehandling,
    ) : KunneIkkeOppdatereSaksopplysninger

    object KanKunStarteOmgjøringDersomViKanInnvilgeMinst1Dag : KunneIkkeOppdatereSaksopplysninger
}
