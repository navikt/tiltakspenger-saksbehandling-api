package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakDb
import java.time.LocalDateTime

private data class DatadelingSakJson(
    val id: String,
    val fnr: String,
    val saksnummer: String,
    val opprettet: LocalDateTime,
)

fun SakDb.toDatadelingJson(): String {
    return DatadelingSakJson(
        id = id.toString(),
        fnr = fnr.verdi,
        saksnummer = saksnummer.verdi,
        opprettet = opprettet,
    ).let { serialize(it) }
}
