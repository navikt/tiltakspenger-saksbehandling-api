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

sealed interface SendSøknadsbehandlingTilBeslutningKommando {
    val sakId: SakId
    val behandlingId: BehandlingId
    val saksbehandler: Saksbehandler
    val correlationId: CorrelationId
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?
    val tiltaksdeltakelser: List<Pair<Periode, String>>
    val automatiskSaksbehandlet: Boolean

    fun valgteTiltaksdeltakelser(behandling: Behandling): ValgteTiltaksdeltakelser {
        return ValgteTiltaksdeltakelser.periodiser(
            tiltaksdeltakelser = tiltaksdeltakelser,
            behandling = behandling,
        )
    }

    fun asInnvilgelseOrNull(): Innvilgelse? = this as? Innvilgelse
    fun asAvslagOrNull(): Avslag? = this as? Avslag

    data class Innvilgelse(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
        override val automatiskSaksbehandlet: Boolean = false,
        override val tiltaksdeltakelser: List<Pair<Periode, String>>,
        val behandlingsperiode: Periode,
        val barnetillegg: Barnetillegg?,
        val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?,
    ) : SendSøknadsbehandlingTilBeslutningKommando

    data class Avslag(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
        override val automatiskSaksbehandlet: Boolean = false,
        override val tiltaksdeltakelser: List<Pair<Periode, String>>,
        val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
    ) : SendSøknadsbehandlingTilBeslutningKommando
}
