package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.periodisering.Periode

sealed interface KanIkkeOppdatereVurderingsperiode {

    data class HarIkkeTilgang(
        val kreverEnAvRollene: Set<Saksbehandlerrolle>,
        val harRollene: Set<Saksbehandlerrolle>,
    ) : KanIkkeOppdatereVurderingsperiode

    data class KanKunKrympe(
        val opprinnligVurderingsperiode: Periode,
    ) : KanIkkeOppdatereVurderingsperiode

    data class BehandlingErSendtTilBeslutterEllerVedtatt(
        val status: Behandlingsstatus,
    ) : KanIkkeOppdatereVurderingsperiode
}
