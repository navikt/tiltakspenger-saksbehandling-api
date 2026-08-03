package no.nav.tiltakspenger.saksbehandling.person.identhendelser.infra.repo

import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.person.Personident

/**
 * Lagres i jsonb-kolonnen `identhendelse.personidenter`.
 * Feltnavnene og identtypene er databaseformatet, og kan ikke endres uten en migrering av det som allerede ligger lagret.
 */
fun List<Personident>.toDbJson(): String = serialize(this)

fun String.fromDbJsonToPersonidenter(): List<Personident> = deserializeList(this)
