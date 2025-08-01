package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface OppdaterSøknadsbehandlingKommando : OppdaterBehandlingKommando {
    override val sakId: SakId
    override val behandlingId: BehandlingId
    override val saksbehandler: Saksbehandler
    override val correlationId: CorrelationId
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?
    val tiltaksdeltakelser: List<Pair<Periode, String>>
    val automatiskSaksbehandlet: Boolean

    fun valgteTiltaksdeltakelser(behandling: Behandling): ValgteTiltaksdeltakelser = ValgteTiltaksdeltakelser.periodiser(
        tiltaksdeltakelser = tiltaksdeltakelser,
        behandling = behandling,
    )

    fun asInnvilgelseOrNull(): Innvilgelse? = this as? Innvilgelse
    fun asAvslagOrNull(): Avslag? = this as? Avslag

    data class Innvilgelse(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
        override val tiltaksdeltakelser: List<Pair<Periode, String>>,
        override val automatiskSaksbehandlet: Boolean = false,
        val innvilgelsesperiode: Periode,
        val barnetillegg: Barnetillegg?,
        val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?,
    ) : OppdaterSøknadsbehandlingKommando

    data class Avslag(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
        override val tiltaksdeltakelser: List<Pair<Periode, String>>,
        override val automatiskSaksbehandlet: Boolean = false,
        val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
    ) : OppdaterSøknadsbehandlingKommando
}
