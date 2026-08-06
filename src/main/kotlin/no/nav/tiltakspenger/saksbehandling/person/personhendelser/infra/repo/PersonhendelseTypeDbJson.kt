package no.nav.tiltakspenger.saksbehandling.person.personhendelser.infra.repo

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.PersonhendelseType
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
private sealed interface PersonhendelseTypeDb {
    data class Doedsfall(
        val doedsdato: LocalDate,
    ) : PersonhendelseTypeDb

    data class Adressebeskyttelse(
        val gradering: String,
    ) : PersonhendelseTypeDb
}

/**
 * Lagres i jsonb-kolonnen `personhendelse.personhendelse_type`.
 * `type`-feltet kommer fra [JsonTypeInfo] og er det variantnavnet som avgjør hvilken variant vi leser tilbake.
 */
fun PersonhendelseType.toDbJson(): String = serialize(
    when (this) {
        is PersonhendelseType.Doedsfall -> PersonhendelseTypeDb.Doedsfall(doedsdato)
        is PersonhendelseType.Adressebeskyttelse -> PersonhendelseTypeDb.Adressebeskyttelse(gradering)
    },
)

fun String.fromDbJsonToPersonhendelseType(): PersonhendelseType =
    when (val personhendelseTypeDb = deserialize<PersonhendelseTypeDb>(this)) {
        is PersonhendelseTypeDb.Doedsfall -> PersonhendelseType.Doedsfall(personhendelseTypeDb.doedsdato)
        is PersonhendelseTypeDb.Adressebeskyttelse -> PersonhendelseType.Adressebeskyttelse(personhendelseTypeDb.gradering)
    }
