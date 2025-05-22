package no.nav.tiltakspenger.saksbehandling.behandling.service.person

sealed interface KunneIkkeHenteEnkelPerson {
    data object FeilVedKallMotPdl : KunneIkkeHenteEnkelPerson
}
