package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

data class InnvilgelsesperiodeKommando(
    val periode: Periode,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    val internDeltakelseId: TiltaksdeltakerId,
)

fun IkkeTomPeriodisering<InnvilgelsesperiodeKommando>.tilInnvilgelsesperioder(behandling: Rammebehandling): Innvilgelsesperioder {
    return Innvilgelsesperioder(
        this.map {
            val tiltaksdeltakelse = behandling.getTiltaksdeltakelse(it.verdi.internDeltakelseId)

            requireNotNull(tiltaksdeltakelse) {
                "Fant ikke tiltaket ${it.verdi.internDeltakelseId} i saksopplysningene for ${behandling.id}"
            }

            InnvilgelsesperiodeVerdi(
                valgtTiltaksdeltakelse = tiltaksdeltakelse,
                antallDagerPerMeldeperiode = it.verdi.antallDagerPerMeldeperiode,
            )
        },
    )
}
