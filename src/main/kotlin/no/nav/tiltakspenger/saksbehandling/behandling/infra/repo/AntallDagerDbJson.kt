package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson

data class AntallDagerDbJson(
    val antallDagerPerMeldeperiode: Int,
    val periode: PeriodeDbJson,
)

// TODO: fjerner etter migrering til innvilgelsesperioder
fun String.tilAntallDagerForMeldeperiode(): List<Pair<Periode, AntallDagerForMeldeperiode>> {
    return deserialize<List<AntallDagerDbJson>>(this).map {
        it.periode.toDomain() to AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode)
    }
}
