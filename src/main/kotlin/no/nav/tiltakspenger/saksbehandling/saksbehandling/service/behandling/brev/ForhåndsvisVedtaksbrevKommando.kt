package no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.brev

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelType
import java.time.LocalDate

data class ForhåndsvisVedtaksbrevKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev,
    val valgteHjemler: List<String>,
    val virkingsperiode: Periode?,
    val barnetillegg: Periodisering<AntallBarn>?,
    val stansDato: LocalDate?,
) {
    fun toValgtHjemmelHarIkkeRettighet(): List<ValgtHjemmelHarIkkeRettighet> {
        return valgteHjemler.map { valgtHjemmel ->
            ValgtHjemmelHarIkkeRettighet.toValgtHjemmelHarIkkeRettighet(
                ValgtHjemmelType.STANS,
                valgtHjemmel,
            )
        }
    }
}
