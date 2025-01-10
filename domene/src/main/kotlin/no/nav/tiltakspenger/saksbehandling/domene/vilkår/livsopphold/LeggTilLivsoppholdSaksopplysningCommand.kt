package no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode

data class LeggTilLivsoppholdSaksopplysningCommand(
    val behandlingId: BehandlingId,
    val saksbehandler: Saksbehandler,
    val harYtelseForPeriode: HarYtelseForPeriode,
    val correlationId: CorrelationId,
) {
    data class HarYtelseForPeriode(
        val periode: Periode,
        val harYtelse: Boolean,
    )
}
