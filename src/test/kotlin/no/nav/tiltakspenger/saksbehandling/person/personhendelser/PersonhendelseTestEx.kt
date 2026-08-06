package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import kotliquery.queryOf
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import no.nav.person.pdl.leesah.Personhendelse as LeesahPersonhendelse

/**
 * Bygger en [LeesahPersonhendelse] slik den kommer inn på pdl.leesah-v1, med en ekstra ident i tillegg til fnr-et.
 * Opplysningstypen utledes av payloaden når den ikke er sendt inn eksplisitt.
 */
fun nyPersonhendelse(
    fnr: Fnr,
    doedsfall: Doedsfall? = null,
    forelderBarnRelasjon: ForelderBarnRelasjon? = null,
    adressebeskyttelse: Adressebeskyttelse? = null,
    opplysningstype: String? = null,
    clock: Clock,
): LeesahPersonhendelse {
    val personidenter = listOf("12345", fnr.verdi)

    val resolvedOpplysningstype = opplysningstype ?: when {
        doedsfall != null -> Opplysningstype.DOEDSFALL_V1.name
        forelderBarnRelasjon != null -> "FORELDERBARNRELASJON_V1"
        else -> Opplysningstype.ADRESSEBESKYTTELSE_V1.name
    }

    return LeesahPersonhendelse(
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

/**
 * `oppgave_sist_sjekket` er køtilstand og skal ikke inn i domenemodellen.
 * Testen leser derfor kolonnen direkte for én hendelse.
 */
fun PostgresSessionFactory.hentPersonhendelseOppgaveSistSjekket(id: UUID): LocalDateTime? =
    withSession {
        it.run(
            queryOf(
                "select oppgave_sist_sjekket from personhendelse where id = :id",
                mapOf("id" to id),
            ).map { row -> row.localDateTime("oppgave_sist_sjekket") }.asSingle,
        )
    }
