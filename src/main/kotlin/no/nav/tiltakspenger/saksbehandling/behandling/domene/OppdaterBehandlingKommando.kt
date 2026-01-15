package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse

sealed interface OppdaterBehandlingKommando {
    val sakId: SakId
    val behandlingId: BehandlingId
    val saksbehandler: Saksbehandler
    val correlationId: CorrelationId
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    val begrunnelseVilkårsvurdering: Begrunnelse?

    sealed interface Innvilgelse {
        val innvilgelsesperioder: IkkeTomPeriodisering<InnvilgelsesperiodeKommando>
        val barnetillegg: Barnetillegg

        fun tilInnvilgelseperioder(behandling: Rammebehandling): Innvilgelsesperioder {
            return Innvilgelsesperioder(
                innvilgelsesperioder.map {
                    val tiltaksdeltakelse = behandling.getTiltaksdeltakelse(it.verdi.internDeltakelseId)

                    requireNotNull(tiltaksdeltakelse) {
                        "Fant ikke tiltaket ${it.verdi.internDeltakelseId} i saksopplysningene for ${behandling.id}"
                    }

                    Innvilgelsesperiode(
                        periode = it.periode,
                        valgtTiltaksdeltakelse = tiltaksdeltakelse,
                        antallDagerPerMeldeperiode = it.verdi.antallDagerPerMeldeperiode,
                    )
                },
            )
        }
    }
}
