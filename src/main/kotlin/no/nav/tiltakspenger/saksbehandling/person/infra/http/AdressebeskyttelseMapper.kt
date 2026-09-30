package no.nav.tiltakspenger.saksbehandling.person.infra.http

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.personklient.pdl.dto.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.personklient.pdl.dto.PdlPersonBolkCode
import no.nav.tiltakspenger.libs.personklient.pdl.dto.avklarGradering
import no.nav.tiltakspenger.saksbehandling.person.Adressebeskyttelse
import tools.jackson.module.kotlin.readValue
import no.nav.tiltakspenger.libs.personklient.pdl.dto.Adressebeskyttelse as PdlAdressebeskyttelse

/** Svaret på bolkoppslaget som bare spør etter adressebeskyttelsen. */
private data class PdlHentAdressebeskyttelseBolkResponse(
    val hentPersonBolk: List<PdlAdressebeskyttelseBolk>,
)

private data class PdlAdressebeskyttelseBolk(
    val ident: String,
    val person: PdlAdressebeskyttelsePerson?,
    val code: PdlPersonBolkCode,
)

private data class PdlAdressebeskyttelsePerson(
    val adressebeskyttelse: List<PdlAdressebeskyttelse>,
)

/**
 * Personene PDL ikke fant, og personene der graderingen ikke kan avklares, er utelatt fra svaret.
 * Kaster hvis svaret ikke kan leses; klienten gjør det om til en feil.
 */
fun String.toAdressebeskyttelseBolk(): Map<Fnr, Adressebeskyttelse> =
    objectMapper.readValue<PdlHentAdressebeskyttelseBolkResponse>(this).hentPersonBolk
        .mapNotNull { bolk ->
            bolk.person
                ?.let { avklarGradering(it.adressebeskyttelse).getOrNull() }
                ?.let { Fnr.fromString(bolk.ident) to it.tilDomene() }
        }
        .toMap()

private fun AdressebeskyttelseGradering.tilDomene(): Adressebeskyttelse = when (this) {
    AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND
    AdressebeskyttelseGradering.STRENGT_FORTROLIG -> Adressebeskyttelse.STRENGT_FORTROLIG
    AdressebeskyttelseGradering.FORTROLIG -> Adressebeskyttelse.FORTROLIG
    AdressebeskyttelseGradering.UGRADERT -> Adressebeskyttelse.UGRADERT
}
