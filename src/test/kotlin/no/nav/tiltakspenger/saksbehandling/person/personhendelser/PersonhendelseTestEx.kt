package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import java.time.Clock
import java.time.Instant

/**
 * Bygger en [Personhendelse] slik den kommer inn på pdl.leesah-v1, med en ekstra ident i tillegg til fnr-et.
 * Opplysningstypen utledes av payloaden når den ikke er sendt inn eksplisitt.
 */
fun nyPersonhendelse(
    fnr: Fnr,
    doedsfall: Doedsfall? = null,
    forelderBarnRelasjon: ForelderBarnRelasjon? = null,
    adressebeskyttelse: Adressebeskyttelse? = null,
    opplysningstype: String? = null,
    clock: Clock,
): Personhendelse {
    val personidenter = listOf("12345", fnr.verdi)

    val resolvedOpplysningstype = opplysningstype ?: when {
        doedsfall != null -> Opplysningstype.DOEDSFALL_V1.name
        forelderBarnRelasjon != null -> "FORELDERBARNRELASJON_V1"
        else -> Opplysningstype.ADRESSEBESKYTTELSE_V1.name
    }

    return Personhendelse(
        "hendelseId",
        personidenter,
        "FREG",
        Instant.now(clock),
        resolvedOpplysningstype,
        Endringstype.OPPRETTET,
        null,
        doedsfall,
        forelderBarnRelasjon,
        adressebeskyttelse,
    )
}
