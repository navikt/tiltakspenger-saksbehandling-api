package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

sealed interface KanIkkeForhåndsviseBrev {
    object FeilMotPdfgenrs : KanIkkeForhåndsviseBrev
}
