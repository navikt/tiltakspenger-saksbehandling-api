package no.nav.tiltakspenger.saksbehandling.person.identhendelser.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.person.Identtype
import no.nav.tiltakspenger.saksbehandling.person.Personident
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * `personidenter` er en jsonb-liste vi serialiserer rett fra domenetypen, og mappingen rører ikke postgres.
 * Å få alle tre identtypene gjennom prodstien ville krevd tre identhendelser for noe som er ren serialisering.
 *
 * Testen pinner **den faktiske json-en**, ikke bare rundturen.
 * En ren rundtur er symmetrisk og ville passert selv om et felt eller en identtype ble omdøpt — og da er radene som allerede ligger i `identhendelse`-tabellen ulesbare uten at noe slår ut.
 * Leseretningen er migreringsdetektoren: den starter fra en literal json slik den ser ut i databasen i dag.
 */
class PersonidenterDbJsonTest {

    @Test
    fun `personidentene lagres med sine avtalte felt- og identtypenavn`() {
        Identtype.entries.map {
            Personident(ident = "12845678911", historisk = false, identtype = it)
        }.toDbJson() shouldBe
            """[{"ident":"12845678911","historisk":false,"identtype":"FOLKEREGISTERIDENT"},{"ident":"12845678911","historisk":false,"identtype":"NPID"},{"ident":"12845678911","historisk":false,"identtype":"AKTORID"}]"""
    }

    @Test
    fun `personidentene leses tilbake fra lagret json`() {
        """[{"ident":"12845678911","historisk":false,"identtype":"FOLKEREGISTERIDENT"},{"ident":"22845678911","historisk":true,"identtype":"NPID"},{"ident":"1234567891011","historisk":false,"identtype":"AKTORID"}]"""
            .fromDbJsonToPersonidenter() shouldBe listOf(
            Personident(ident = "12845678911", historisk = false, identtype = Identtype.FOLKEREGISTERIDENT),
            Personident(ident = "22845678911", historisk = true, identtype = Identtype.NPID),
            Personident(ident = "1234567891011", historisk = false, identtype = Identtype.AKTORID),
        )
    }
}
