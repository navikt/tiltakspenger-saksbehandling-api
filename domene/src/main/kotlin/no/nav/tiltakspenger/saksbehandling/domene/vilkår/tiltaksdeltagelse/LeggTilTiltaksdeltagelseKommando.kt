package no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.tilstøter
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.ÅrsakTilEndring

/**
 * Vi støtter ikke å krympe inn tiltaksdeltagelse vilkåret. Man må sette en status for hele den originale perioden.
 */
data class LeggTilTiltaksdeltagelseKommando(
    val sakId: SakId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val behandlingId: BehandlingId,
    val statusForPeriode: NonEmptyList<StatusForPeriode>,
    val årsakTilEndring: ÅrsakTilEndring?,
) {
    val perioder: List<Periode> = statusForPeriode.map { it.periode }

    val totalPeriode = Periode(
        fraOgMed = perioder.first().fraOgMed,
        tilOgMed = perioder.last().tilOgMed,
    )

    val antallPerioder = statusForPeriode.size

    init {
        require(perioder.tilstøter() && perioder.sortedWith(compareBy<Periode> { it.fraOgMed }.thenBy { it.tilOgMed }) == perioder) {
            "Periodene må tilstøte hverandre og være sortert. Kan ikke ha duplikater eller overlapp. Var: $perioder"
        }
        require(antallPerioder == 1) {
            "Et krav i første versjon er at antall perioder må være 1"
        }
    }

    data class StatusForPeriode(
        val periode: Periode,
        val status: TiltakDeltakerstatus,
    )
}
