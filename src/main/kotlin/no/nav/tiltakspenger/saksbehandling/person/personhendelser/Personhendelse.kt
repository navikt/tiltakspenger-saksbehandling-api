package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import java.time.LocalDate
import java.util.UUID

data class Personhendelse(
    val id: UUID,
    val fnr: Fnr,
    val hendelseId: String,
    val opplysningstype: Opplysningstype,
    val personhendelseType: PersonhendelseType,
    val sakId: SakId,
) {
    fun gjelderAdressebeskyttelse() = opplysningstype == Opplysningstype.ADRESSEBESKYTTELSE_V1
}

/**
 * Subset av PDL sine opplysningstyper på pdl.leesah-v1 som vi faktisk håndterer.
 * Andre typer filtreres bort uten kall til databasen.
 */
enum class Opplysningstype {
    DOEDSFALL_V1,
    ADRESSEBESKYTTELSE_V1,
}

sealed interface PersonhendelseType {
    data class Doedsfall(
        val doedsdato: LocalDate,
    ) : PersonhendelseType

    data class Adressebeskyttelse(
        val gradering: String,
    ) : PersonhendelseType
}
