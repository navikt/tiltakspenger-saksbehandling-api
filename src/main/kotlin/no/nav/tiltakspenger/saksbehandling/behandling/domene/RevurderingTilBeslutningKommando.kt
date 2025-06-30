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

sealed interface RevurderingTilBeslutningKommando {
    val sakId: SakId
    val behandlingId: BehandlingId
    val saksbehandler: Saksbehandler
    val correlationId: CorrelationId
    val begrunnelse: BegrunnelseVilkårsvurdering
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
}

data class RevurderingStansTilBeslutningKommando(
    override val sakId: SakId,
    override val behandlingId: BehandlingId,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
    override val begrunnelse: BegrunnelseVilkårsvurdering,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val valgteHjemler: NonEmptyList<ValgtHjemmelForStans>,
    val stansFraOgMed: LocalDate,
    // Bestemmes av tidligere vedtak på saken, må settes når behandlingen sendes til beslutning
    val sisteDagSomGirRett: LocalDate?,
) : RevurderingTilBeslutningKommando

data class RevurderingInnvilgelseTilBeslutningKommando(
    override val sakId: SakId,
    override val behandlingId: BehandlingId,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
    override val begrunnelse: BegrunnelseVilkårsvurdering,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val innvilgelsesperiode: Periode,
    val tiltaksdeltakelser: List<Pair<Periode, String>>,
    val barnetillegg: Barnetillegg?,
    val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>,
) : RevurderingTilBeslutningKommando
