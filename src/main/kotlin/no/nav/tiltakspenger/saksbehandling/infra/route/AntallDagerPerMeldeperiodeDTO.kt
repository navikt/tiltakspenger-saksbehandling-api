package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode

data class AntallDagerPerMeldeperiodeDTO(
    val periode: PeriodeDTO,
    val antallDagerPerMeldeperiode: Int,
)
fun Periodisering<AntallDagerForMeldeperiode>.toDTO(): List<AntallDagerPerMeldeperiodeDTO> {
    return this.perioderMedVerdi.map {
        AntallDagerPerMeldeperiodeDTO(antallDagerPerMeldeperiode = it.verdi.value, periode = it.periode.toDTO())
    }
}
