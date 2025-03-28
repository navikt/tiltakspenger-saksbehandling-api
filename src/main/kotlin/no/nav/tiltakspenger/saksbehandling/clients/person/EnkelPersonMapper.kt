package no.nav.tiltakspenger.saksbehandling.clients.person

import arrow.core.Either
import arrow.core.getOrElse
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.personklient.pdl.dto.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.personklient.pdl.dto.Navn
import no.nav.tiltakspenger.libs.personklient.pdl.dto.PdlPerson
import no.nav.tiltakspenger.libs.personklient.pdl.dto.avklarGradering
import no.nav.tiltakspenger.libs.personklient.pdl.dto.avklarNavn
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.EnkelPerson

internal data class PdlResponse(
    val hentPerson: PdlPerson,
)

private val logger = KotlinLogging.logger { }

fun String.toEnkelPerson(
    fnr: Fnr,
): EnkelPerson {
    val data: PdlResponse = Either.catch {
        objectMapper.readValue<PdlResponse>(this)
    }.getOrElse {
        logger.error { "Klarte ikke deserialisere respons fra pdl. Se sikkerlog for mer informasjon" }
        sikkerlogg.error(it) { "Klarte ikke deserialisere respons fra pdl. fnr ${fnr.verdi} respons: $this " }
        throw it
    }
    val person: PdlPerson = data.hentPerson
    val navn: Navn = avklarNavn(person.navn).getOrElse { it.mapError() }
    val adressebeskyttelse: AdressebeskyttelseGradering =
        avklarGradering(person.adressebeskyttelse).getOrElse { it.mapError() }
    return EnkelPerson(
        fnr = fnr,
        fornavn = navn.fornavn,
        mellomnavn = navn.mellomnavn,
        etternavn = navn.etternavn,
        fortrolig = adressebeskyttelse.erFortrolig(),
        strengtFortrolig = adressebeskyttelse.erStrengtFortrolig(),
        strengtFortroligUtland = adressebeskyttelse.erStrengtFortroligUtland(),
    )
}
