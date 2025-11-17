package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.service

sealed interface KunneIkkeHenteTiltaksdeltakelser {
    data object FeilVedKallMotPdl : KunneIkkeHenteTiltaksdeltakelser
}
