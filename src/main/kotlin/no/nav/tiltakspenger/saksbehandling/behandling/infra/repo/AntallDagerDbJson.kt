package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson

data class AntallDagerDbJson(
    val antallDagerPerMeldeperiode: Int,
    val periode: PeriodeDbJson,
)

fun Innvilgelsesperioder.tilAntallDagerForMeldeperiodeDbJson(): String {
    return this.antallDagerPerMeldeperiode.perioderMedVerdi.map {
        AntallDagerDbJson(
            antallDagerPerMeldeperiode = it.verdi.value,
            periode = it.periode.toDbJson(),
        )
    }.serialize()
}

fun String.tilAntallDagerForMeldeperiode(): List<Pair<Periode, AntallDagerForMeldeperiode>> {
    return deserialize<List<AntallDagerDbJson>>(this).map {
        it.periode.toDomain() to AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode)
    }
}
