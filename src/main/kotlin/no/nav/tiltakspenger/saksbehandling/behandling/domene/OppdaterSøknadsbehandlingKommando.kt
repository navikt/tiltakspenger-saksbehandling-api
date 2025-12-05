package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.ValgteTiltaksdeltakelser

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
        override val tiltaksdeltakelser: List<Pair<Periode, String>>,
        override val innvilgelsesperiode: Periode,
        override val barnetillegg: Barnetillegg,
        override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>,
    ) : OppdaterSøknadsbehandlingKommando,
        OppdaterBehandlingKommando.Innvilgelse {

        fun valgteTiltaksdeltakelser(behandling: Rammebehandling): ValgteTiltaksdeltakelser =
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
        override val begrunnelseVilkårsvurdering: Begrunnelse?,
        override val automatiskSaksbehandlet: Boolean,
        val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
    ) : OppdaterSøknadsbehandlingKommando

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
