package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.tilSammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson

data class AntallDagerDbJson(
    val antallDagerPerMeldeperiode: Int,
    val periode: PeriodeDbJson,
)

fun Periodisering<AntallDagerForMeldeperiode>.toDbJson(): String {
    return this.perioderMedVerdi.toList().map {
        AntallDagerDbJson(
            antallDagerPerMeldeperiode = it.verdi.value,
            periode = it.periode.toDbJson(),
        )
    }.serialize()
}

fun String.toAntallDagerForMeldeperiode(): SammenhengendePeriodisering<AntallDagerForMeldeperiode> {
    return deserialize<List<AntallDagerDbJson>>(this).map {
        PeriodeMedVerdi(AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode), it.periode.toDomain())
    }.tilSammenhengendePeriodisering()
}
