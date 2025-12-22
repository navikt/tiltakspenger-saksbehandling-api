package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.trekkFra
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando

/**
 * * @param barnetillegg  Kan inneholde hull, men kan ikke være tom. Må valideres basert på innsendt [innvilgelsesperioder].
 */
data class ForhåndsvisVedtaksbrevForSøknadsbehandlingInnvilgelseKommando(
    override val sakId: SakId,
    override val behandlingId: BehandlingId,
    override val correlationId: CorrelationId,
    override val saksbehandler: Saksbehandler,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val innvilgelsesperioder: IkkeTomPeriodisering<InnvilgelsesperiodeKommando>,
    val barnetillegg: IkkeTomPeriodisering<AntallBarn>?,
) : ForhåndsvisVedtaksbrevForSøknadsbehandlingKommando {
    val antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode> = innvilgelsesperioder.map {
        it.verdi.antallDagerPerMeldeperiode
    }.slåSammenTilstøtendePerioder()

    init {
        if (barnetillegg != null) {
            require(barnetillegg.perioder.trekkFra(innvilgelsesperioder.perioder).isEmpty()) {
                "Barnetillegg kan ikke inneholde perioder utenfor innvilgelsesperiodene"
            }
        }
    }
}
