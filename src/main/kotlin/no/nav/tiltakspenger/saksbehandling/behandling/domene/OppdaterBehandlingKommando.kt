package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg

sealed interface OppdaterBehandlingKommando {
    val sakId: SakId
    val behandlingId: BehandlingId
    val saksbehandler: Saksbehandler
    val correlationId: CorrelationId
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?

    /** Er kun satt for Søknadsbehandling/Revurdering til innvilgelse */
    val innvilgelsesperiode: Periode?

    /** Er kun satt for Søknadsbehandling/Revurdering til innvilgelse */
    val barnetillegg: Barnetillegg?

    /** Er kun satt for Søknadsbehandling/Revurdering til innvilgelse */
    val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?

    /** Er kun satt for Søknadsbehandling/Revurdering til innvilgelse */
    val tiltaksdeltakelser: List<Pair<Periode, String>>?
}
