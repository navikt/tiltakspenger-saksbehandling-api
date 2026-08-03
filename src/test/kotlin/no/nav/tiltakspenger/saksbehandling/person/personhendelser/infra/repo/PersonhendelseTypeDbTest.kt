package no.nav.tiltakspenger.saksbehandling.person.personhendelser.infra.repo

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * `personhendelse_type` er en polymorf jsonb-verdi med én variant per opplysningstype, og mappingen rører ikke postgres.
 * Å produsere begge variantene gjennom prodstien ville krevd to hele hendelsesløp for noe som er ren serialisering.
 *
 * Testen pinner **den faktiske json-en**, ikke bare rundturen.
 * En ren rundtur er symmetrisk og ville passert selv om en variant ble omdøpt — og da er raden som allerede ligger i `personhendelse`-tabellen ulesbar uten at noe slår ut.
 * Leseretningen er migreringsdetektoren: den starter fra en literal json slik den ser ut i databasen i dag.
 */
class PersonhendelseTypeDbTest {

    @Test
    fun `doedsfall lagres med sitt avtalte variantnavn`() {
        PersonhendelseType.Doedsfall(doedsdato = LocalDate.of(2025, 3, 17)).toDbJson() shouldBe
            """{"type":"Doedsfall","doedsdato":"2025-03-17"}"""
    }

    @Test
    fun `adressebeskyttelse lagres med sitt avtalte variantnavn`() {
        PersonhendelseType.Adressebeskyttelse(gradering = "STRENGT_FORTROLIG").toDbJson() shouldBe
            """{"type":"Adressebeskyttelse","gradering":"STRENGT_FORTROLIG"}"""
    }

    @Test
    fun `doedsfall leses tilbake fra lagret json`() {
        """{"type":"Doedsfall","doedsdato":"2025-03-17"}""".fromDbJsonToPersonhendelseType() shouldBe
            PersonhendelseType.Doedsfall(doedsdato = LocalDate.of(2025, 3, 17))
    }

    @Test
    fun `adressebeskyttelse leses tilbake fra lagret json`() {
        """{"type":"Adressebeskyttelse","gradering":"STRENGT_FORTROLIG"}""".fromDbJsonToPersonhendelseType() shouldBe
            PersonhendelseType.Adressebeskyttelse(gradering = "STRENGT_FORTROLIG")
    }
}
