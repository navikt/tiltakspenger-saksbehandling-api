package no.nav.tiltakspenger.saksbehandling.søknad.domene

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class BarnetilleggFraSøknadTest {

    @Test
    fun `norge og annet EØS-land er innenfor EØS, ikke-EØS-land er utenfor`() {
        Oppholdssted.NORGE.tilOppholderSegIEØS() shouldBe Søknad.JaNeiSpm.Ja
        Oppholdssted.ANNET_EØS_LAND.tilOppholderSegIEØS() shouldBe Søknad.JaNeiSpm.Ja
        Oppholdssted.IKKE_EØS_LAND.tilOppholderSegIEØS() shouldBe Søknad.JaNeiSpm.Nei
    }

    @Test
    fun `barn uten oppholdssted godtas, slik eldre søknader er`() {
        ObjectMother.barnetilleggMedIdent(oppholderSegIEØS = Søknad.JaNeiSpm.Nei, oppholdssted = null)
            .oppholdssted shouldBe null
        ObjectMother.barnetilleggUtenIdent(oppholderSegIEØS = Søknad.JaNeiSpm.IkkeBesvart, oppholdssted = null)
            .oppholdssted shouldBe null
    }

    @Test
    fun `oppholdssted som ikke stemmer med ja-nei-svaret gir feil`() {
        shouldThrowWithMessage<IllegalArgumentException>("oppholderSegIEØS=Ja stemmer ikke med oppholdssted=IKKE_EØS_LAND") {
            ObjectMother.barnetilleggMedIdent(oppholderSegIEØS = Søknad.JaNeiSpm.Ja, oppholdssted = Oppholdssted.IKKE_EØS_LAND)
        }
        shouldThrowWithMessage<IllegalArgumentException>("oppholderSegIEØS=Nei stemmer ikke med oppholdssted=ANNET_EØS_LAND") {
            ObjectMother.barnetilleggUtenIdent(oppholderSegIEØS = Søknad.JaNeiSpm.Nei, oppholdssted = Oppholdssted.ANNET_EØS_LAND)
        }
    }

    @Test
    fun `bare barn i ikke-EØS-land gir barn utenfor EØS`() {
        fun søknadMedBarnI(oppholdssted: Oppholdssted) = ObjectMother.nyInnvilgbarSøknad(
            barnetillegg = listOf(
                ObjectMother.barnetilleggMedIdent(
                    oppholderSegIEØS = oppholdssted.tilOppholderSegIEØS(),
                    oppholdssted = oppholdssted,
                ),
            ),
        )

        søknadMedBarnI(Oppholdssted.NORGE).harBarnUtenforEOS() shouldBe false
        søknadMedBarnI(Oppholdssted.ANNET_EØS_LAND).harBarnUtenforEOS() shouldBe false
        søknadMedBarnI(Oppholdssted.IKKE_EØS_LAND).harBarnUtenforEOS() shouldBe true
    }
}
