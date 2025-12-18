package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando.Innvilgelse.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperioderDTO
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
                    val tiltaksdeltakelse = behandling.getTiltaksdeltakelse(it.verdi.tiltaksdeltakelseId)

                    requireNotNull(tiltaksdeltakelse) {
                        "Fant ikke tiltaket ${it.verdi.tiltaksdeltakelseId} i saksopplysningene for ${behandling.id}"
                    }

                    Innvilgelsesperiode(
                        periode = it.periode,
                        valgtTiltaksdeltakelse = tiltaksdeltakelse,
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(it.verdi.antallDagerPerMeldeperiode),
                    )
                },
            )
        }

        data class InnvilgelsesperiodeKommando(
            val periode: Periode,
            val antallDagerPerMeldeperiode: Int,
            val tiltaksdeltakelseId: String,
        )
    }
}

fun InnvilgelsesperioderDTO.tilKommando(): IkkeTomPeriodisering<InnvilgelsesperiodeKommando> {
    return this.map {
        val periode = it.periode.toDomain()

        PeriodeMedVerdi(
            periode = periode,
            verdi = InnvilgelsesperiodeKommando(
                periode = periode,
                antallDagerPerMeldeperiode = it.antallDagerPerMeldeperiode,
                tiltaksdeltakelseId = it.tiltaksdeltakelseId,
            ),
        )
    }.tilIkkeTomPeriodisering()
}
