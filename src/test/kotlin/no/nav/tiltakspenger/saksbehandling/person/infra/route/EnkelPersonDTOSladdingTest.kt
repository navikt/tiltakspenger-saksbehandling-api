package no.nav.tiltakspenger.saksbehandling.person.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.infra.route.SLADDET_TEKST
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.EnkelPersonMedSkjerming
import org.junit.jupiter.api.Test

class EnkelPersonDTOSladdingTest {

    @Test
    fun `alle personopplysningsfelter i EnkelPersonDTO erstattes mens øvrige felter forblir uendret`() {
        val enkelPersonDTO = enkelPersonDTO()

        enkelPersonDTO.sladdet() shouldBe enkelPersonDTO.copy(
            fnr = SLADDET_TEKST,
            fødselsdato = SLADDET_TEKST,
            fornavn = SLADDET_TEKST,
            mellomnavn = SLADDET_TEKST,
            etternavn = SLADDET_TEKST,
            dødsdato = SLADDET_TEKST,
        )
    }

    @Test
    fun `flaggene for adressebeskyttelse og skjerming beholdes`() {
        enkelPersonDTO().sladdet().let {
            it.fortrolig shouldBe false
            it.strengtFortrolig shouldBe true
            it.strengtFortroligUtland shouldBe false
            it.skjermet shouldBe true
        }
    }

    @Test
    fun `nullbare felter forblir null`() {
        val utenMellomnavnOgDødsdato = enkelPersonDTO().copy(mellomnavn = null, dødsdato = null)

        utenMellomnavnOgDødsdato.sladdet().mellomnavn shouldBe null
        utenMellomnavnOgDødsdato.sladdet().dødsdato shouldBe null
    }

    @Test
    fun `veileder får urørte personopplysninger, mens utvikler får dem sladdet`() {
        val enkelPersonDTO = enkelPersonDTO()

        enkelPersonDTO.sladdetFor(ObjectMother.veileder()) shouldBe enkelPersonDTO
        enkelPersonDTO.sladdetFor(ObjectMother.utvikler()) shouldBe enkelPersonDTO.sladdet()

        listOf(enkelPersonDTO).sladdetFor(ObjectMother.veileder()) shouldBe listOf(enkelPersonDTO)
        listOf(enkelPersonDTO).sladdetFor(ObjectMother.utvikler()) shouldBe listOf(enkelPersonDTO.sladdet())
    }

    private fun enkelPersonDTO(): EnkelPersonDTO =
        EnkelPersonMedSkjerming(ObjectMother.personopplysningMaxFyr(), erSkjermet = true)
            .toEnkelPersonDTO()
            .copy(dødsdato = 1.januar(2024).toString())
}
