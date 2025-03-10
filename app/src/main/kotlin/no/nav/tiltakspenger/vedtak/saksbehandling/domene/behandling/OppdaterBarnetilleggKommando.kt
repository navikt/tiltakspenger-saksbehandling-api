package no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.vedtak.barnetillegg.AntallBarn
import no.nav.tiltakspenger.vedtak.barnetillegg.Barnetillegg

data class OppdaterBarnetilleggKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val begrunnelse: BegrunnelseVilkårsvurdering?,
    val perioder: List<Pair<Periode, AntallBarn>>,
) {
    fun barnetillegg(virkningsperiode: Periode?): Barnetillegg {
        return Barnetillegg.periodiserOgFyllUtHullMed0(
            perioder = perioder,
            begrunnelse = begrunnelse,
            virkningsperiode = virkningsperiode,
        )
    }
}
