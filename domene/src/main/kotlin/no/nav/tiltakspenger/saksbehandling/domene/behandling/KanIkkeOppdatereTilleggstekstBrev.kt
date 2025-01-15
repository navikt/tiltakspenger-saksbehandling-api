package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle

sealed interface KanIkkeOppdatereTilleggstekstBrev {

    data class HarIkkeTilgang(
        val kreverEnAvRollene: Set<Saksbehandlerrolle>,
        val harRollene: Set<Saksbehandlerrolle>,
    ) : KanIkkeOppdatereTilleggstekstBrev

    data class BehandlingErSendtTilBeslutterEllerVedtatt(
        val status: Behandlingsstatus,
    ) : KanIkkeOppdatereTilleggstekstBrev
}
