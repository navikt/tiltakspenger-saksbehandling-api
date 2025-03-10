package no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.vedtak.barnetillegg.AntallBarn
import no.nav.tiltakspenger.vedtak.barnetillegg.Barnetillegg

data class SendSøknadsbehandlingTilBeslutningKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
    val innvilgelsesperiode: Periode,
    val begrunnelse: BegrunnelseVilkårsvurdering?,
    val perioder: List<Pair<Periode, AntallBarn>>?,
) {
    fun barnetillegg(): Barnetillegg? {
        return perioder?.let {
            Barnetillegg.periodiserOgFyllUtHullMed0(
                perioder = it,
                begrunnelse = begrunnelse,
                virkningsperiode = innvilgelsesperiode,
            )
        }
    }
}
