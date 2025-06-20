package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

// TODO: split innvilgelse og avslag
data class SendSøknadsbehandlingTilBeslutningKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
    val behandlingsperiode: Periode,
    val barnetillegg: Barnetillegg?,
    val tiltaksdeltakelser: List<Pair<Periode, String>>,
    val antallDagerPerMeldeperiode: Int,
    val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>?,
    val resultat: SøknadsbehandlingType,
    val automatiskSaksbehandlet: Boolean = false,
) {
    fun valgteTiltaksdeltakelser(behandling: Behandling): ValgteTiltaksdeltakelser {
        return ValgteTiltaksdeltakelser.periodiser(
            tiltaksdeltakelser = tiltaksdeltakelser,
            behandling = behandling,
        )
    }
}
