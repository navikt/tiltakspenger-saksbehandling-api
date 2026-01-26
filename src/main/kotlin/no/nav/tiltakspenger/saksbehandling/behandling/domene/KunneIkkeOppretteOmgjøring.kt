package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KunneIkkeOppretteOmgjøring {
    object KanKunStarteOmgjøringDersomViKanInnvilgeMinst1Dag : KunneIkkeOppretteOmgjøring
    object PerioderSomOmgjøresMåVæreSammenhengede : KunneIkkeOppretteOmgjøring
}
