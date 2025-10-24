package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.service

sealed interface KunneIkkeHenteTiltaksdeltakelser {
    data object FeilVedKallMotPdl : KunneIkkeHenteTiltaksdeltakelser
}
