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

sealed interface RevurderingTilBeslutningKommando : SendBehandlingTilBeslutningKommando {
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
        // Bestemmes av tidligere vedtak på saken, må settes når behandlingen sendes til beslutning
        val sisteDagSomGirRett: LocalDate?,
    ) : RevurderingTilBeslutningKommando

    data class Innvilgelse(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        val innvilgelsesperiode: Periode,
        val tiltaksdeltakelser: List<Pair<Periode, String>>,
        val barnetillegg: Barnetillegg?,
        val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>,
    ) : RevurderingTilBeslutningKommando
}
