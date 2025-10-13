package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.core.Either
import arrow.core.getOrElse
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.personklient.pdl.dto.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.personklient.pdl.dto.ForelderBarnRelasjon
import no.nav.tiltakspenger.libs.personklient.pdl.dto.Navn
import no.nav.tiltakspenger.libs.personklient.pdl.dto.PdlPerson
import no.nav.tiltakspenger.libs.personklient.pdl.dto.PdlPersonBolk
import no.nav.tiltakspenger.libs.personklient.pdl.dto.avklarFødsel
import no.nav.tiltakspenger.libs.personklient.pdl.dto.avklarGradering
import no.nav.tiltakspenger.libs.personklient.pdl.dto.avklarNavn
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson

internal data class PdlHentPersonResponse(
    val hentPerson: PdlPerson,
)

internal data class PdlHentPersonBolkResponse(
    val hentPersonBolk: List<PdlPersonBolk>,
)

private val logger = KotlinLogging.logger { }

fun PdlPerson.toEnkelPerson(fnr: Fnr): EnkelPerson {
    val navn: Navn = avklarNavn(this.navn).getOrElse { it.mapError() }
    val fødsel = avklarFødsel(this.foedselsdato).getOrElse { it.mapError() }
    val adressebeskyttelse: AdressebeskyttelseGradering =
        avklarGradering(this.adressebeskyttelse).getOrElse { it.mapError() }

    return EnkelPerson(
        fnr = fnr,
        fødselsdato = fødsel.foedselsdato,
        fornavn = navn.fornavn,
        mellomnavn = navn.mellomnavn,
        etternavn = navn.etternavn,
        fortrolig = adressebeskyttelse.erFortrolig(),
        strengtFortrolig = adressebeskyttelse.erStrengtFortrolig(),
        strengtFortroligUtland = adressebeskyttelse.erStrengtFortroligUtland(),
        dødsdato = this.doedsfall.lastOrNull()?.doedsdato,
    )
}

fun String.toEnkelPerson(
    fnr: Fnr,
): EnkelPerson {
    val data: PdlHentPersonResponse = Either.catch {
        objectMapper.readValue<PdlHentPersonResponse>(this)
    }.getOrElse {
        logger.error { "Klarte ikke deserialisere respons fra pdl. Se sikkerlog for mer informasjon" }
        Sikkerlogg.error(it) { "Klarte ikke deserialisere respons fra pdl. fnr ${fnr.verdi} respons: $this " }
        throw it
    }
    return data.hentPerson.toEnkelPerson(fnr = fnr)
}

fun String.toEnkelPersonBolk(fnr: List<Fnr>): List<EnkelPerson> {
    val data: PdlHentPersonBolkResponse = Either.catch {
        objectMapper.readValue<PdlHentPersonBolkResponse>(this)
    }.getOrElse {
        logger.error { "Klarte ikke deserialisere respons fra pdl. Se sikkerlog for mer informasjon" }
        Sikkerlogg.error(it) { "Klarte ikke deserialisere respons fra pdl. fnr $fnr respons: $this " }
        throw it
    }

    val enkelPersoner = data.hentPersonBolk.map {
        val person = it.person
        if (person == null) {
            logger.error { "Feil ved henting av person med bolkoppslag fra PDL, code=${it.code}" }
            Sikkerlogg.error { "Feil ved henting av person med bolkoppslag fra PDL, code=${it.code}, ident=${it.ident}" }
            return@map null
        }

        person.toEnkelPerson(fnr = Fnr.fromString(it.ident))
    }
    return enkelPersoner.filterNotNull()
}

fun String.toForelderBarnRelasjon(fnr: Fnr): List<ForelderBarnRelasjon> {
    val data: PdlHentPersonResponse = try {
        objectMapper.readValue<PdlHentPersonResponse>(this)
    } catch (e: Exception) {
        logger.error { "Klarte ikke deserialisere respons fra pdl. Se sikkerlog for mer informasjon" }
        Sikkerlogg.error(e) { "Klarte ikke deserialisere respons fra pdl. fnr ${fnr.verdi} respons: $this " }
        throw e
    }
    return data.hentPerson.forelderBarnRelasjon
}
