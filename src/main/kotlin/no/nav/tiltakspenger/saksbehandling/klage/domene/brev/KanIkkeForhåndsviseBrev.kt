package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

sealed interface KanIkkeForhåndsviseBrev {
    object FeilMotPdfgen : KanIkkeForhåndsviseBrev
}
