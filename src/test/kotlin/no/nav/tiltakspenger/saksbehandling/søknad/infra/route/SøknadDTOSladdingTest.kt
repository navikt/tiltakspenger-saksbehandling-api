package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.infra.route.SLADDET_TEKST
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class SøknadDTOSladdingTest {

    @Test
    fun `alle personopplysningsfelter i SøknadDTO erstattes mens øvrige felter forblir uendret`() {
        val søknadDTO = søknadDTO()

        søknadDTO.sladdet() shouldBe søknadDTO.copy(
            barnetillegg = søknadDTO.barnetillegg.map { it.sladdet() },
            avbrutt = søknadDTO.avbrutt?.copy(begrunnelse = SLADDET_TEKST),
        )
    }

    @Test
    fun `barna sladdes uansett om de har fødselsnummer eller ikke`() {
        val barnetillegg = søknadDTO().sladdet().barnetillegg

        barnetillegg.map { it.fornavn } shouldBe listOf(SLADDET_TEKST, SLADDET_TEKST)
        barnetillegg.map { it.mellomnavn } shouldBe listOf(SLADDET_TEKST, SLADDET_TEKST)
        barnetillegg.map { it.etternavn } shouldBe listOf(SLADDET_TEKST, SLADDET_TEKST)
        barnetillegg.map { it.fødselsdato } shouldBe listOf(SLADDET_TEKST, SLADDET_TEKST)
        barnetillegg.map { it.fnr } shouldBe listOf(SLADDET_TEKST, null)
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
            ObjectMother.barnetilleggMedIdent(fnr = Fnr.random()),
            ObjectMother.barnetilleggUtenIdent(),
        ),
        avbrutt = Avbrutt(
            tidspunkt = 1.januarDateTime(2025),
            saksbehandler = "Z12345",
            begrunnelse = "Søker har trukket søknaden".toNonBlankString(),
        ),
    ).toSøknadDTO()
}
