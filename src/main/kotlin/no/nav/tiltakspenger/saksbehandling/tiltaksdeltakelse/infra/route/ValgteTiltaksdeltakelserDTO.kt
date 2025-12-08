package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse

data class TiltaksdeltakelsePeriodeDTO(
    val eksternDeltagelseId: String,
    val periode: PeriodeDTO,
)

fun Periodisering<Tiltaksdeltakelse>.toTiltaksdeltakelsePeriodeDTO(): List<TiltaksdeltakelsePeriodeDTO> {
    return this.perioderMedVerdi.map {
        TiltaksdeltakelsePeriodeDTO(
            eksternDeltagelseId = it.verdi.eksternDeltakelseId,
            periode = it.periode.toDTO(),
        )
    }
}

fun List<TiltaksdeltakelsePeriodeDTO>.tilDomene(): List<Pair<Periode, String>> {
    return this.map {
        it.periode.toDomain() to it.eksternDeltagelseId
    }
}
