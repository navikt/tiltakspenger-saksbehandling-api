package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import java.time.LocalDate

data class SendRevurderingTilBeslutningKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
    val begrunnelse: BegrunnelseVilkårsvurdering,
    val valgteHjemler: List<String>,
    val stansDato: LocalDate,
)
