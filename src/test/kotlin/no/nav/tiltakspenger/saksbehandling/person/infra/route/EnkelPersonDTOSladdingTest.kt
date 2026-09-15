package no.nav.tiltakspenger.saksbehandling.person.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.EnkelPersonMedSkjerming
import org.junit.jupiter.api.Test

class EnkelPersonDTOSladdingTest {

    @Test
    fun `alle personopplysningsfelter i EnkelPersonDTO sladdes mens øvrige felter forblir uendret`() {
        val enkelPersonDTO = enkelPersonDTO()

        enkelPersonDTO.sladdet() shouldBe enkelPersonDTO.copy(
            fnr = SladdetVerdi,
            fødselsdato = SladdetVerdi,
            fornavn = SladdetVerdi,
            mellomnavn = SladdetVerdi,
            etternavn = SladdetVerdi,
            dødsdato = SladdetVerdi,
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
    fun `felter som mangler sladdes på linje med de utfylte`() {
        val utenMellomnavnOgDødsdato = enkelPersonDTO().copy(
            mellomnavn = null.ikkeSladdet(),
            dødsdato = null.ikkeSladdet(),
        )

        utenMellomnavnOgDødsdato.sladdet().mellomnavn shouldBe SladdetVerdi
        utenMellomnavnOgDødsdato.sladdet().dødsdato shouldBe SladdetVerdi
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
            .copy(dødsdato = 1.januar(2024).ikkeSladdet())
}
