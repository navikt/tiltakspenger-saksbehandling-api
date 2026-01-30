package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

data class InnvilgelsesperiodeDTO(
    val periode: PeriodeDTO,
    val antallDagerPerMeldeperiode: Int,
    val internDeltakelseId: String,
)

typealias InnvilgelsesperioderDTO = List<InnvilgelsesperiodeDTO>

fun InnvilgelsesperioderDTO.tilKommando(): IkkeTomPeriodisering<InnvilgelsesperiodeKommando> {
    return this.map {
        val periode = it.periode.toDomain()

        PeriodeMedVerdi(
            periode = periode,
            verdi = InnvilgelsesperiodeKommando(
                periode = periode,
                antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                internDeltakelseId = TiltaksdeltakerId.fromString(it.internDeltakelseId),
            ),
        )
    }.tilIkkeTomPeriodisering()
}

fun Innvilgelsesperioder.tilDTO(): InnvilgelsesperioderDTO {
    return this.periodisering.perioderMedVerdi.map {
        InnvilgelsesperiodeDTO(
            periode = it.periode.toDTO(),
            antallDagerPerMeldeperiode = it.verdi.antallDagerPerMeldeperiode.value,
            internDeltakelseId = it.verdi.valgtTiltaksdeltakelse.internDeltakelseId.toString(),
        )
    }
}
