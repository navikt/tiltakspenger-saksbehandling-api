package no.nav.tiltakspenger.saksbehandling.person

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr

/**
 * Fødselsnumrene og d-numrene én person har eller har hatt ifølge PDL.
 * Typen sier bare at numrene hører til samme person, og skal ikke brukes til tilgangskontroll eller til å bytte fnr på saken.
 *
 * @param gjeldende Nummeret saken står på.
 * @param tidligere De andre numrene personen har eller har hatt, uten [gjeldende] og uten duplikater.
 */
data class Fødselsnumre(
    val gjeldende: Fnr,
    val tidligere: List<Fnr> = emptyList(),
) {
    init {
        require(gjeldende !in tidligere) { "Det gjeldende fødselsnummeret kan ikke også stå blant de tidligere" }
        require(tidligere.distinct().size == tidligere.size) { "De tidligere fødselsnumrene kan ikke ha duplikater" }
    }

    /** Alle numrene, med [gjeldende] først. */
    val alle: NonEmptyList<Fnr> get() = nonEmptyListOf(gjeldende) + tidligere
}
