package no.nav.tiltakspenger.saksbehandling.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.json.objectMapper
import org.postgresql.util.PGobject

internal fun <T> String.hent(
    params: Map<String, Any> = emptyMap(),
    session: Session,
    rowMapping: (Row) -> T,
): T? = session.run(queryOf(this, params).map { row -> rowMapping(row) }.asSingle)

internal fun <T> String.hentListe(
    params: Map<String, Any> = emptyMap(),
    session: Session,
    rowMapping: (Row) -> T,
): List<T> = session.run(queryOf(this, params).map { row -> rowMapping(row) }.asList)

internal fun Row.booleanOrNull(name: String): Boolean? = this.anyOrNull(name)?.let { this.boolean(name) }

fun toPGObject(value: Any?) = PGobject().also {
    it.type = "json"
    it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
}
