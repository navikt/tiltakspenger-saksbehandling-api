package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Oppholdssted
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelser
import org.junit.jupiter.api.Test

class SøknadDTOSladdingTest {

    @Test
    fun `alle personopplysningsfelter i SøknadDTO sladdes mens øvrige felter forblir uendret`() {
        val søknadDTO = søknadDTO()

        søknadDTO.sladdet() shouldBe søknadDTO.copy(
            barnetillegg = søknadDTO.barnetillegg.map { it.sladdet() },
            avbrutt = søknadDTO.avbrutt.map { it.copy(begrunnelse = SladdetVerdi) },
        )
    }

    @Test
    fun `barna sladdes uansett om de har fødselsnummer eller ikke`() {
        val barnetillegg = søknadDTO().sladdet().barnetillegg

        barnetillegg.map { it.fornavn } shouldBe listOf(SladdetVerdi, SladdetVerdi)
        barnetillegg.map { it.mellomnavn } shouldBe listOf(SladdetVerdi, SladdetVerdi)
        barnetillegg.map { it.etternavn } shouldBe listOf(SladdetVerdi, SladdetVerdi)
        barnetillegg.map { it.fødselsdato } shouldBe listOf(SladdetVerdi, SladdetVerdi)
        barnetillegg.map { it.fnr } shouldBe listOf(SladdetVerdi, SladdetVerdi)
        barnetillegg.map { it.oppholdssted } shouldBe listOf(
            SøknadDTO.OppholdsstedDTO.NORGE,
            SøknadDTO.OppholdsstedDTO.IKKE_EØS_LAND,
        )
    }

    @Test
    fun `journalpostId, tiltak og svarene i søknaden beholdes`() {
        val søknadDTO = søknadDTO()

        søknadDTO.sladdet().let {
            it.journalpostId shouldBe søknadDTO.journalpostId
            it.tiltak shouldBe søknadDTO.tiltak
            it.svar shouldBe søknadDTO.svar
        }
    }

    @Test
    fun `veileder får urørt søknad, mens utvikler får den sladdet`() {
        val søknadDTO = søknadDTO()

        søknadDTO.sladdetFor(ObjectMother.veileder()) shouldBe søknadDTO
        søknadDTO.sladdetFor(ObjectMother.utvikler()) shouldBe søknadDTO.sladdet()
    }

    private fun søknadDTO(): SøknadDTO = ObjectMother.nyInnvilgbarSøknad(
        barnetillegg = listOf(
            ObjectMother.barnetilleggMedIdent(fnr = Fnr.random(), oppholdssted = Oppholdssted.NORGE),
            ObjectMother.barnetilleggUtenIdent(oppholderSegIEØS = Søknad.JaNeiSpm.Nei, oppholdssted = Oppholdssted.IKKE_EØS_LAND),
        ),
        avbrutt = Søknadshendelser.fromAvbrutt(
            tidspunkt = 1.januarDateTime(2025),
            utførtAv = "Z12345",
            begrunnelse = "Søker har trukket søknaden".toNonBlankString(),
        ),
    ).toSøknadDTO()
}
