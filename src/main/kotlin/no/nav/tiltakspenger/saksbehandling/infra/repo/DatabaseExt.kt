package no.nav.tiltakspenger.saksbehandling.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.json.objectMapper
import org.postgresql.util.PGobject

fun Row.booleanOrNull(name: String): Boolean? = this.anyOrNull(name)?.let { this.boolean(name) }

fun toPGObject(value: Any?) = PGobject().also {
    it.type = "json"
    it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
}
