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
    val automatiskSaksbehandlet: Boolean

    fun asInnvilgelseOrNull(): Innvilgelse? = this as? Innvilgelse

    data class Innvilgelse(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
        override val automatiskSaksbehandlet: Boolean = false,
        val tiltaksdeltakelser: List<Pair<Periode, String>>,
        val innvilgelsesperiode: Periode,
        val barnetillegg: Barnetillegg?,
        val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?,
    ) : OppdaterSøknadsbehandlingKommando {

        fun valgteTiltaksdeltakelser(behandling: Behandling): ValgteTiltaksdeltakelser =
            ValgteTiltaksdeltakelser.periodiser(
                tiltaksdeltakelser = tiltaksdeltakelser,
                behandling = behandling,
            )
    }

    data class Avslag(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
        override val automatiskSaksbehandlet: Boolean = false,
        val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
    ) : OppdaterSøknadsbehandlingKommando

    data class IkkeValgtResultat(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
    ) : OppdaterSøknadsbehandlingKommando {

        override val automatiskSaksbehandlet: Boolean = false
    }
}
