package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson

private data class ValgteTiltaksdeltakelserDbJson(
    val value: List<TiltaksdeltakelsePeriodeMedVerdi>,
)

private data class TiltaksdeltakelsePeriodeMedVerdi(
    val periode: PeriodeDbJson,
    val eksternDeltagelseId: String,
)

// TODO: fjernes etter migrering til innvilgelsesperioder
fun String.tilValgteTiltaksdeltakelser(): List<Pair<Periode, String>> {
    return deserialize<ValgteTiltaksdeltakelserDbJson>(this).value.map {
        it.periode.toDomain() to it.eksternDeltagelseId
    }
}
