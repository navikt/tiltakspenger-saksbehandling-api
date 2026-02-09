package no.nav.tiltakspenger.saksbehandling.felles

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.nå
import org.junit.jupiter.api.Test

class VentestatusTest {

    @Test
    fun `tom liste er gyldig og erSattPåVent er false`() {
        val ventestatus = Ventestatus()

        ventestatus.erSattPåVent shouldBe false
        ventestatus.ventestatusHendelser shouldBe emptyList()
    }

    @Test
    fun `kan sette på vent`() {
        val clock = TikkendeKlokke()
        val ventestatus = Ventestatus()
            .settPåVent(nå(clock), "saksbehandler", "Venter på dokumentasjon", "UNDER_BEHANDLING")

        ventestatus.erSattPåVent shouldBe true
        ventestatus.ventestatusHendelser.size shouldBe 1
    }

    @Test
    fun `kan sette på vent og gjenoppta`() {
        val clock = TikkendeKlokke()
        val ventestatus = Ventestatus()
            .settPåVent(nå(clock), "saksbehandler", "Venter på dokumentasjon", "UNDER_BEHANDLING")
            .gjenoppta(nå(clock), "saksbehandler", "UNDER_BEHANDLING")

        ventestatus.erSattPåVent shouldBe false
        ventestatus.ventestatusHendelser.size shouldBe 2
    }

    @Test
    fun `kan sette på vent, gjenoppta og sette på vent igjen`() {
        val clock = TikkendeKlokke()
        val ventestatus = Ventestatus()
            .settPåVent(nå(clock), "saksbehandler", "Venter på dokumentasjon", "UNDER_BEHANDLING")
            .gjenoppta(nå(clock), "saksbehandler", "UNDER_BEHANDLING")
            .settPåVent(nå(clock), "saksbehandler", "Venter på mer info", "UNDER_BEHANDLING")

        ventestatus.erSattPåVent shouldBe true
        ventestatus.ventestatusHendelser.size shouldBe 3
    }

    @Test
    fun `første hendelse må være sattPåVent=true`() {
        val clock = TikkendeKlokke()
        shouldThrow<IllegalArgumentException> {
            Ventestatus(
                ventestatusHendelser = listOf(
                    VentestatusHendelse(
                        tidspunkt = nå(clock),
                        endretAv = "saksbehandler",
                        begrunnelse = "",
                        frist = null,
                        erSattPåVent = false,
                        status = "UNDER_BEHANDLING",
                    ),
                ),
            )
        }.message shouldBe "erSattPåVent må alternere, og første hendelse må være sattPåVent=true"
    }

    @Test
    fun `hendelser må alternere - kan ikke sette på vent to ganger på rad`() {
        val clock = TikkendeKlokke()
        shouldThrow<IllegalArgumentException> {
            Ventestatus(
                ventestatusHendelser = listOf(
                    VentestatusHendelse(
                        tidspunkt = nå(clock),
                        endretAv = "saksbehandler",
                        begrunnelse = "Begrunnelse 1",
                        frist = null,
                        erSattPåVent = true,
                        status = "UNDER_BEHANDLING",
                    ),
                    VentestatusHendelse(
                        tidspunkt = nå(clock),
                        endretAv = "saksbehandler",
                        begrunnelse = "Begrunnelse 2",
                        frist = null,
                        erSattPåVent = true,
                        status = "UNDER_BEHANDLING",
                    ),
                ),
            )
        }.message shouldBe "erSattPåVent må alternere, og første hendelse må være sattPåVent=true"
    }

    @Test
    fun `hendelser må alternere - kan ikke gjenoppta to ganger på rad`() {
        val clock = TikkendeKlokke()
        shouldThrow<IllegalArgumentException> {
            Ventestatus(
                ventestatusHendelser = listOf(
                    VentestatusHendelse(
                        tidspunkt = nå(clock),
                        endretAv = "saksbehandler",
                        begrunnelse = "Begrunnelse",
                        frist = null,
                        erSattPåVent = true,
                        status = "UNDER_BEHANDLING",
                    ),
                    VentestatusHendelse(
                        nå(clock = clock),
                        "saksbehandler",
                        "",
                        frist = null,
                        erSattPåVent = false,
                        "UNDER_BEHANDLING",
                    ),
                    VentestatusHendelse(
                        nå(clock = clock),
                        "saksbehandler",
                        "",
                        frist = null,
                        erSattPåVent = false,
                        "UNDER_BEHANDLING",
                    ),
                ),
            )
        }.message shouldBe "erSattPåVent må alternere, og første hendelse må være sattPåVent=true"
    }

    @Test
    fun `hendelser må være sortert etter tidspunkt`() {
        val clock = TikkendeKlokke()
        val liktTidspunkt = nå(clock)
        shouldThrow<IllegalArgumentException> {
            Ventestatus(
                ventestatusHendelser = listOf(
                    VentestatusHendelse(
                        tidspunkt = liktTidspunkt,
                        endretAv = "saksbehandler",
                        begrunnelse = "Begrunnelse",
                        frist = null,
                        erSattPåVent = true,
                        status = "UNDER_BEHANDLING",
                    ),
                    VentestatusHendelse(
                        tidspunkt = liktTidspunkt,
                        endretAv = "saksbehandler",
                        begrunnelse = "",
                        frist = null,
                        erSattPåVent = false,
                        status = "UNDER_BEHANDLING",
                    ),
                ),
            )
        }.message shouldBe "Hendelsene må være sortert etter tidspunkt"
    }
}
