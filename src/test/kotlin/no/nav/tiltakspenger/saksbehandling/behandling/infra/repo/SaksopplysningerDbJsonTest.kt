package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

/**
 * Ren mapping testes uten database.
 * Saksopplysninger lagret før tiltaksdeltakelser som ikke gir rett ble tatt med, mangler feltet, og kan ikke produseres av noen prodsti i dag.
 */
class SaksopplysningerDbJsonTest {

    @Test
    fun `tiltaksdeltakelser som ikke gir rett lagres og leses tilbake`() {
        val lønnstilskudd = ObjectMother.tiltaksdeltakelseSomFratarRett(eksternDeltakelseId = "TA123")
        val saksopplysninger = ObjectMother.saksopplysninger(tiltaksdeltakelserSomFratarRett = listOf(lønnstilskudd))

        val lagret = saksopplysninger.toDbJson()

        lagret shouldContain
            """"tiltaksdeltakelserSomFratarRett":[{"eksternDeltakelseId":"TA123","typeNavn":"Midlertidig lønnstilskudd","tiltakskodeFraKilden":"MIDLONTIL","deltakelseFraOgMed":"2023-01-01","deltakelseTilOgMed":"2023-03-31","deltakelseStatus":"Deltar","deltakelseProsent":100.0,"antallDagerPerUke":5.0,"kilde":"Arena"}]"""
        lagret.toSaksopplysninger() shouldBe saksopplysninger
    }

    @Test
    fun `saksopplysninger lagret uten feltet leses som ingen tiltaksdeltakelser som ikke gir rett`() {
        val lagret = ObjectMother.saksopplysninger().toDbJson()
        val lagretFørFeltetFantes = lagret.replace(""""tiltaksdeltakelserSomFratarRett":[],""", "")

        lagretFørFeltetFantes.contains("tiltaksdeltakelserSomFratarRett") shouldBe false
        lagretFørFeltetFantes.toSaksopplysninger().tiltaksdeltakelserSomFratarRett.shouldBeEmpty()
    }
}
