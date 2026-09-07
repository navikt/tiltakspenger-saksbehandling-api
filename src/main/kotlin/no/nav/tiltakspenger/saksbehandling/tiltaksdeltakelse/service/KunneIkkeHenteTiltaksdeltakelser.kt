package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.service

sealed interface KunneIkkeHenteTiltaksdeltakelser {
    data object FeilVedKallMotPdl : KunneIkkeHenteTiltaksdeltakelser

    /**
     * Oppslaget mot tiltakshistorikk (evt. identoppslaget mot PDL foran det) feilet.
     * Feilen logges i [TiltaksdeltakelseService], så varianten trenger ikke bære den.
     */
    data object FeilVedKallMotTiltak : KunneIkkeHenteTiltaksdeltakelser

    data object OppslagsperiodeMangler : KunneIkkeHenteTiltaksdeltakelser

    data object NegativOppslagsperiode : KunneIkkeHenteTiltaksdeltakelser
}
