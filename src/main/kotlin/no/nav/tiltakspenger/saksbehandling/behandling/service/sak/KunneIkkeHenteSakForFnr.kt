package no.nav.tiltakspenger.saksbehandling.behandling.service.sak

sealed interface KunneIkkeHenteSakForFnr {
    data object FantIkkeSakForFnr : KunneIkkeHenteSakForFnr
}
