package no.nav.tiltakspenger.saksbehandling.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf

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
