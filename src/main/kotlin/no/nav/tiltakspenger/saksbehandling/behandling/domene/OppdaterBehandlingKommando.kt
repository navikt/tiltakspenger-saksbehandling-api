package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg

sealed interface OppdaterBehandlingKommando {
    val sakId: SakId
    val behandlingId: BehandlingId
    val saksbehandler: Saksbehandler
    val correlationId: CorrelationId
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?

    sealed interface Innvilgelse {
        val innvilgelsesperiode: Periode
        val barnetillegg: Barnetillegg
        val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>
        val tiltaksdeltakelser: List<Pair<Periode, String>>
    }
}
