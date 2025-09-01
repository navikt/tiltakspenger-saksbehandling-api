package no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class PersonhendelseDb(
    val id: UUID,
    val fnr: Fnr,
    val hendelseId: String,
    val opplysningstype: Opplysningstype,
    val personhendelseType: PersonhendelseType,
    val sakId: SakId,
    val oppgaveId: OppgaveId?,
    val oppgaveSistSjekket: LocalDateTime?,
) {
    fun gjelderAdressebeskyttelse() = opplysningstype == Opplysningstype.ADRESSEBESKYTTELSE_V1
}

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface PersonhendelseType {
    data class Doedsfall(
        val doedsdato: LocalDate,
    ) : PersonhendelseType

    data class ForelderBarnRelasjon(
        val relatertPersonsIdent: String,
        val minRolleForPerson: String,
    ) : PersonhendelseType

    data class Adressebeskyttelse(
        val gradering: String,
    ) : PersonhendelseType
}
