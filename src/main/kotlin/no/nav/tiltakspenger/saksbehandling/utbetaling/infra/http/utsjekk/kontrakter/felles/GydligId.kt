package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles

object GyldigSakId {
    const val MAKSLENGDE = 25
    const val BESKRIVELSE = "På grunn av tekniske begrensninger hos OS/UR er det en lengdebegrensning på $MAKSLENGDE tegn for sakId"
}

object GyldigBehandlingId {
    const val MAKSLENGDE = 30
    const val BESKRIVELSE = "På grunn av tekniske begrensninger hos OS/UR er det en lengdebegrensning på $MAKSLENGDE tegn for behandlingId"
}
