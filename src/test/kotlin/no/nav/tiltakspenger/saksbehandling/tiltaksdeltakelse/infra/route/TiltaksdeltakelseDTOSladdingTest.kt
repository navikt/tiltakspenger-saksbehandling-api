package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class TiltaksdeltakelseDTOSladdingTest {

    @Test
    fun `tittelen til en deltakelse som gir rett sladdes mens typenavnet beholdes`() {
        val dto = ObjectMother.tiltaksdeltakelse(tittel = "Avklaring hos Arrangør AS").toDTO()

        dto.sladdet() shouldBe dto.copy(tittel = SladdetVerdi)
    }

    @Test
    fun `tittelen til en deltakelse som fratar rett sladdes mens typenavnet beholdes`() {
        val dto = ObjectMother.tiltaksdeltakelseSomFratarRett(tittel = "Lønnstilskudd hos Arbeidsgiver AS").toDTO()

        dto.sladdet() shouldBe dto.copy(tittel = SladdetVerdi)
    }
}
