package no.nav.tiltakspenger.saksbehandling.domene.vilkår.kvp

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.Deltagelse

data class LeggTilKvpSaksopplysningCommand(
    val behandlingId: BehandlingId,
    val saksbehandler: Saksbehandler,
    val deltakelseForPeriode: List<DeltakelseForPeriode>,
    val correlationId: CorrelationId,
) {
    data class DeltakelseForPeriode(
        val periode: Periode,
        val deltar: Boolean,
    ) {
        fun tilDeltagelse(): Deltagelse = if (deltar) Deltagelse.DELTAR else Deltagelse.DELTAR_IKKE
    }
}
