package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import java.time.LocalDate
import java.time.LocalDateTime

private data class DatadelingMeldeperioderJson(
    val sakId: String,
    val meldeperioder: List<DatadelingMeldeperiode>,
) {
    data class DatadelingMeldeperiode(
        val id: String,
        val kjedeId: String,
        val opprettet: LocalDateTime,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val antallDagerForPeriode: Int,
        val girRett: Map<LocalDate, Boolean>,
    )
}

fun List<Meldeperiode>.toDatadelingJson(sakId: SakId): String {
    return DatadelingMeldeperioderJson(
        sakId = sakId.toString(),
        meldeperioder = this.map { it.toDatadelingMeldeperiode() },
    ).let { serialize(it) }
}

private fun Meldeperiode.toDatadelingMeldeperiode() = DatadelingMeldeperioderJson.DatadelingMeldeperiode(
    id = id.toString(),
    kjedeId = kjedeId.toString(),
    opprettet = opprettet,
    fraOgMed = periode.fraOgMed,
    tilOgMed = periode.tilOgMed,
    antallDagerForPeriode = maksAntallDagerForMeldeperiode,
    girRett = girRett,
)
