package no.nav.tiltakspenger.saksbehandling.klage.domene

sealed interface KanIkkeForhåndsviseBrev {
    object FeilMotPdfgen : KanIkkeForhåndsviseBrev
}
