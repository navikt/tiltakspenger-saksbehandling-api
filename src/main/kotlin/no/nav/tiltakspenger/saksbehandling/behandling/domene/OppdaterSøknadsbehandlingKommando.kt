package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando.Innvilgelse.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse

sealed interface OppdaterSøknadsbehandlingKommando : OppdaterBehandlingKommando {
    override val sakId: SakId
    override val behandlingId: BehandlingId
    override val saksbehandler: Saksbehandler
    override val correlationId: CorrelationId
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    override val begrunnelseVilkårsvurdering: Begrunnelse?
    val automatiskSaksbehandlet: Boolean

    data class Innvilgelse(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val begrunnelseVilkårsvurdering: Begrunnelse?,
        override val automatiskSaksbehandlet: Boolean,
        override val innvilgelsesperioder: IkkeTomPeriodisering<InnvilgelsesperiodeKommando>,
        override val barnetillegg: Barnetillegg,
    ) : OppdaterSøknadsbehandlingKommando,
        OppdaterBehandlingKommando.Innvilgelse

    data class Avslag(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val begrunnelseVilkårsvurdering: Begrunnelse?,
        val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
    ) : OppdaterSøknadsbehandlingKommando {
        override val automatiskSaksbehandlet: Boolean = false
    }

    /**
     * Brukes av saksbehndler til å lagre fritekst og begrunnelse før de har valgt et resultat (innvilgelse/avslag).
     */
    data class IkkeValgtResultat(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val saksbehandler: Saksbehandler,
        override val correlationId: CorrelationId,
        override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
        override val begrunnelseVilkårsvurdering: Begrunnelse?,
    ) : OppdaterSøknadsbehandlingKommando {
        override val automatiskSaksbehandlet: Boolean = false
    }
}
