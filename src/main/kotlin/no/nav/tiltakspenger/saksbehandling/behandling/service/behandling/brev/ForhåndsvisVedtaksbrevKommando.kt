package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsutfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelHarIkkeRettighet
import java.time.LocalDate

data class ForhåndsvisVedtaksbrevKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev,
    val valgteHjemler: List<ValgtHjemmelHarIkkeRettighet>,
    val virkingsperiode: Periode?,
    val barnetillegg: Periodisering<AntallBarn>?,
    val stansDato: LocalDate?,
    val utfall: Behandlingsutfall,
    val avslagsgrunner: Set<Avslagsgrunnlag>,
) {
    init {
        if (utfall == Behandlingsutfall.AVSLAG || avslagsgrunner.isNotEmpty()) {
            require(utfall == Behandlingsutfall.AVSLAG) { "Behandlingsutfall må være AVSLAG når det er valgt avslagsgrunner" }
            require(avslagsgrunner.isNotEmpty()) { "Det må være valgt avslagsgrunner når behandlingsutfall er AVSLAG" }
        }
    }
}
