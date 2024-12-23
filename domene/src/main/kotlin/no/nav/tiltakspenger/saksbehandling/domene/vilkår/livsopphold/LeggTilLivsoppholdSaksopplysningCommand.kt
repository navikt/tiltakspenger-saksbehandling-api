package no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.ÅrsakTilEndring

data class LeggTilLivsoppholdSaksopplysningCommand(
    val behandlingId: BehandlingId,
    val saksbehandler: Saksbehandler,
    val harYtelseForPeriode: HarYtelseForPeriode,
    val årsakTilEndring: ÅrsakTilEndring?,
    val correlationId: CorrelationId,
) {
    data class HarYtelseForPeriode(
        val periode: Periode,
        val harYtelse: Boolean,
    )
}
