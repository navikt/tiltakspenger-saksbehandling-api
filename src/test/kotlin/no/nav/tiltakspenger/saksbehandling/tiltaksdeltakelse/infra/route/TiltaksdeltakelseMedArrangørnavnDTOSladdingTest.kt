package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.infra.route.SLADDET_TEKST
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class TiltaksdeltakelseMedArrangørnavnDTOSladdingTest {

    @Test
    fun `visningsnavnet med arrangørnavn erstattes mens tiltakstypen forblir uendret`() {
        val tiltaksdeltakelseDTO = ObjectMother.tiltaksdeltakelseMedArrangørnavn().toDTO()

        tiltaksdeltakelseDTO.sladdet() shouldBe tiltaksdeltakelseDTO.copy(visningsnavn = SLADDET_TEKST)
        tiltaksdeltakelseDTO.sladdet().typeNavn shouldBe tiltaksdeltakelseDTO.typeNavn
    }

    @Test
    fun `veileder får urørt visningsnavn, mens utvikler får det sladdet`() {
        val tiltaksdeltakelseDTO = ObjectMother.tiltaksdeltakelseMedArrangørnavn().toDTO()

        tiltaksdeltakelseDTO.sladdetFor(ObjectMother.veileder()) shouldBe tiltaksdeltakelseDTO
        tiltaksdeltakelseDTO.sladdetFor(ObjectMother.utvikler()) shouldBe tiltaksdeltakelseDTO.sladdet()

        listOf(tiltaksdeltakelseDTO).sladdetFor(ObjectMother.veileder()) shouldBe listOf(tiltaksdeltakelseDTO)
        listOf(tiltaksdeltakelseDTO).sladdetFor(ObjectMother.utvikler()) shouldBe
            listOf(tiltaksdeltakelseDTO.sladdet())
    }
}
