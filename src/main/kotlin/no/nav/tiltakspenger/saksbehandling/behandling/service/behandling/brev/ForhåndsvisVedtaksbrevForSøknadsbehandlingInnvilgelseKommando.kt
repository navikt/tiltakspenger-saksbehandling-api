package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.ValgtBarnetillegg

/**
 * @param barnetillegg Barnetillegget i skjemaet, som valideres mot innvilgelsesperiodene ved forhåndsvisning.
 * Brukes bare når saksbehandler kan saksbehandle behandlingen, ellers brukes det lagrede barnetillegget.
 * Null betyr ingen barnetillegg.
 */
data class ForhåndsvisVedtaksbrevForSøknadsbehandlingInnvilgelseKommando(
    override val sakId: SakId,
    override val behandlingId: RammebehandlingId,
    override val correlationId: CorrelationId,
    override val saksbehandler: Saksbehandler,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val innvilgelsesperioder: IkkeTomPeriodisering<InnvilgelsesperiodeKommando>,
    val barnetillegg: ValgtBarnetillegg?,
) : ForhåndsvisVedtaksbrevForSøknadsbehandlingKommando {
    val antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode> = innvilgelsesperioder.map {
        it.verdi.antallDagerPerMeldeperiode
    }.slåSammenTilstøtendePerioder()
}
