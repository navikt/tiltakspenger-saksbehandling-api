package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class SladdingTest {

    @Test
    fun `fagrollene gir personinnsyn, mens utvikler og bruker uten roller ikke gjør det`() {
        skalSladdeFor(ObjectMother.saksbehandler()) shouldBe false
        skalSladdeFor(ObjectMother.beslutter()) shouldBe false
        skalSladdeFor(ObjectMother.veileder()) shouldBe false
        skalSladdeFor(
            ObjectMother.saksbehandler(roller = Saksbehandlerroller(listOf(Saksbehandlerrolle.TILBAKEKREVING))),
        ) shouldBe false

        skalSladdeFor(ObjectMother.utvikler()) shouldBe true
        skalSladdeFor(ObjectMother.saksbehandlerUtenTilgang()) shouldBe true
    }

    @Test
    fun `bruker med både utvikler og saksbehandlerrolle får ikke sladdet`() {
        skalSladdeFor(
            ObjectMother.saksbehandler(
                roller = Saksbehandlerroller(
                    listOf(Saksbehandlerrolle.UTVIKLER, Saksbehandlerrolle.SAKSBEHANDLER),
                ),
            ),
        ) shouldBe false
    }

    @Test
    fun `begrunnelsen i attesteringen erstattes, og null forblir null`() {
        val attestering = AttesteringDTO(
            endretAv = "B12345",
            status = Attesteringsstatus.SENDT_TILBAKE,
            begrunnelse = "Mangler dokumentasjon på bostedet til barnet",
            endretTidspunkt = 1.januarDateTime(2025),
        )

        attestering.sladdet() shouldBe attestering.copy(begrunnelse = SLADDET_TEKST)
        attestering.copy(begrunnelse = null).sladdet().begrunnelse shouldBe null
    }

    @Test
    fun `begrunnelsen i avbruttfeltet erstattes`() {
        val avbrutt = AvbruttDTO(
            avbruttAv = "Z12345",
            avbruttTidspunkt = 1.januarDateTime(2025).toString(),
            begrunnelse = "Søker har flyttet til en annen kommune",
        )

        avbrutt.sladdet() shouldBe avbrutt.copy(begrunnelse = SLADDET_TEKST)
    }

    @Test
    fun `begrunnelsen i ventestatushendelsen erstattes`() {
        val ventestatusHendelse = VentestatusHendelseDTO(
            sattPåVentAv = "Z12345",
            tidspunkt = 1.januarDateTime(2025).toString(),
            status = "UNDER_BEHANDLING",
            begrunnelse = "Venter på legeerklæring",
            erSattPåVent = true,
            frist = null,
        )

        ventestatusHendelse.sladdet() shouldBe ventestatusHendelse.copy(begrunnelse = SLADDET_TEKST)
    }
}
