package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import java.time.LocalDate

sealed interface OppdaterRevurderingKommando : OppdaterBehandlingKommando {
    override val sakId: SakId
    override val behandlingId: BehandlingId
    override val saksbehandler: Saksbehandler
    override val correlationId: CorrelationId
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering

    data class Stans(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        val valgteHjemler: NonEmptyList<ValgtHjemmelForStans>,
        val stansFraOgMed: LocalDate,
    ) : OppdaterRevurderingKommando

    data class Innvilgelse(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val innvilgelsesperiode: Periode,
        override val tiltaksdeltakelser: List<Pair<Periode, String>>,
        override val barnetillegg: Barnetillegg,
        override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>,
    ) : OppdaterRevurderingKommando,
        OppdaterBehandlingKommando.Innvilgelse
}
