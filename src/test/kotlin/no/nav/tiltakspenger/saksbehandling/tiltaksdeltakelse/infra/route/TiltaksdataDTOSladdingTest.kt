package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdata
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import org.junit.jupiter.api.Test

class TiltaksdataDTOSladdingTest {

    private val tiltaksdataDTO = Tiltaksdata(
        eksternDeltakelseId = "TA1",
        internDeltakelseId = TiltaksdeltakerId.random(),
        tittel = "Sveisekurs hos Arrangør AS",
    ).toDTO()

    @Test
    fun `tittelen erstattes mens id-ene forblir uendret`() {
        tiltaksdataDTO.sladdet() shouldBe tiltaksdataDTO.copy(tittel = SladdetVerdi)
    }

    @Test
    fun `veileder får urørt tittel, mens utvikler får den sladdet`() {
        listOf(tiltaksdataDTO).sladdetFor(ObjectMother.veileder()) shouldBe listOf(tiltaksdataDTO)
        listOf(tiltaksdataDTO).sladdetFor(ObjectMother.utvikler()) shouldBe listOf(tiltaksdataDTO.sladdet())
    }
}
