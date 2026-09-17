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
    fun `saksbehandler, beslutter og tilbakekreving kan se benken, mens veileder, utvikler og bruker uten roller ikke kan det`() {
        kanSeBenken(ObjectMother.saksbehandler()) shouldBe true
        kanSeBenken(ObjectMother.beslutter()) shouldBe true
        kanSeBenken(
            ObjectMother.saksbehandler(roller = Saksbehandlerroller(listOf(Saksbehandlerrolle.TILBAKEKREVING))),
        ) shouldBe true

        kanSeBenken(ObjectMother.veileder()) shouldBe false
        kanSeBenken(ObjectMother.utvikler()) shouldBe false
        kanSeBenken(ObjectMother.saksbehandlerUtenTilgang()) shouldBe false
    }

    @Test
    fun `bruker med både utvikler og saksbehandlerrolle kan se benken`() {
        kanSeBenken(
            ObjectMother.saksbehandler(
                roller = Saksbehandlerroller(
                    listOf(Saksbehandlerrolle.UTVIKLER, Saksbehandlerrolle.SAKSBEHANDLER),
                ),
            ),
        ) shouldBe true
    }

    @Test
    fun `begrunnelsen i attesteringen sladdes, også når den mangler`() {
        val attestering = AttesteringDTO(
            endretAv = "B12345",
            status = Attesteringsstatus.SENDT_TILBAKE,
            begrunnelse = "Mangler dokumentasjon på bostedet til barnet".ikkeSladdet(),
            endretTidspunkt = 1.januarDateTime(2025),
        )

        attestering.sladdet() shouldBe attestering.copy(begrunnelse = SladdetVerdi)
        attestering.copy(begrunnelse = null.ikkeSladdet()).sladdet().begrunnelse shouldBe SladdetVerdi
    }

    @Test
    fun `begrunnelsen i avbruttfeltet sladdes`() {
        val avbrutt = AvbruttDTO(
            avbruttAv = "Z12345",
            avbruttTidspunkt = 1.januarDateTime(2025).toString(),
            begrunnelse = "Søker har flyttet til en annen kommune".ikkeSladdet(),
        )

        avbrutt.sladdet() shouldBe avbrutt.copy(begrunnelse = SladdetVerdi)
    }

    @Test
    fun `begrunnelsen i ventestatushendelsen sladdes`() {
        val ventestatusHendelse = VentestatusHendelseDTO(
            sattPåVentAv = "Z12345",
            tidspunkt = 1.januarDateTime(2025).toString(),
            status = "UNDER_BEHANDLING",
            begrunnelse = "Venter på legeerklæring".ikkeSladdet(),
            erSattPåVent = true,
            frist = null,
        )

        ventestatusHendelse.sladdet() shouldBe ventestatusHendelse.copy(begrunnelse = SladdetVerdi)
    }
}
