package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.barnetillegg.AntallBarn
import no.nav.tiltakspenger.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode

data class OppdaterBarnetilleggKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val begrunnelse: BegrunnelseVilkårsvurdering?,
    val perioder: List<Pair<Periode, AntallBarn>>,
) {
    fun barnetillegg(virkningsperiode: Periode): Barnetillegg {
        return Barnetillegg.periodiserOgFyllUtHullMed0(
            perioder = perioder,
            begrunnelse = begrunnelse,
            virkningsperiode = virkningsperiode,
        )
    }
}
