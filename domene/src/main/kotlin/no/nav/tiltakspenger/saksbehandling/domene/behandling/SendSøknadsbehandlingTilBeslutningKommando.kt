package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.barnetillegg.AntallBarn
import no.nav.tiltakspenger.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.ValgteTiltaksdeltakelser

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
    val tiltaksdeltakelser: List<Pair<Periode, String>>,
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

    fun valgteTiltaksdeltakelser(behandling: Behandling): ValgteTiltaksdeltakelser {
        return ValgteTiltaksdeltakelser.periodiser(
            tiltaksdeltakelser = tiltaksdeltakelser,
            behandling = behandling,
        )
    }
}
