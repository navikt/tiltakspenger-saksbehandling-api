package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson

private data class ValgteTiltaksdeltakelserDbJson(
    val value: List<TiltaksdeltakelsePeriodeMedVerdi>,
)

private data class TiltaksdeltakelsePeriodeMedVerdi(
    val periode: PeriodeDbJson,
    val eksternDeltagelseId: String,
)

fun String.tilValgteTiltaksdeltakelser(): List<Pair<Periode, String>> {
    return deserialize<ValgteTiltaksdeltakelserDbJson>(this).value.map {
        it.periode.toDomain() to it.eksternDeltagelseId
    }
}

fun Innvilgelsesperioder.tilValgteTiltaksdeltakelserDbJson(): String {
    return ValgteTiltaksdeltakelserDbJson(
        value = this.valgteTiltaksdeltagelser.perioderMedVerdi.toList().map {
            TiltaksdeltakelsePeriodeMedVerdi(
                periode = it.periode.toDbJson(),
                eksternDeltagelseId = it.verdi.eksternDeltakelseId,
            )
        },
    ).let { serialize(it) }
}
