package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder

data class InnvilgelsesperiodeDTO(
    val periode: PeriodeDTO,
    val antallDagerPerMeldeperiode: Int,
    val tiltaksdeltakelseId: String,
)

typealias InnvilgelsesperioderDTO = NonEmptyList<InnvilgelsesperiodeDTO>

fun Innvilgelsesperioder.tilDTO(): InnvilgelsesperioderDTO {
    return this.periodisering.perioderMedVerdi.map {
        InnvilgelsesperiodeDTO(
            periode = it.periode.toDTO(),
            antallDagerPerMeldeperiode = it.verdi.antallDagerPerMeldeperiode.value,
            tiltaksdeltakelseId = it.verdi.valgtTiltaksdeltakelse.eksternDeltakelseId,
        )
    }
}
