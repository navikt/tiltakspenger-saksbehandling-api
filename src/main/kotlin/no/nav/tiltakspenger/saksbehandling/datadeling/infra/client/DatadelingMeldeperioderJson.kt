package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate
import java.time.LocalDateTime

private data class DatadelingMeldeperioderJson(
    val fnr: String,
    val sakId: String,
    val saksnummer: String,
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

fun List<Meldeperiode>.toDatadelingJson(sak: Sak): String {
    return DatadelingMeldeperioderJson(
        fnr = sak.fnr.verdi,
        sakId = sak.id.toString(),
        saksnummer = sak.saksnummer.verdi,
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
