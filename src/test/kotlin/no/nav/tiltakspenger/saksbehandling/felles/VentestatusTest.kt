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
                    VentestatusHendelse(nå(clock), "saksbehandler", "", erSattPåVent = false, "UNDER_BEHANDLING"),
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
                        nå(clock),
                        "saksbehandler",
                        "Begrunnelse 1",
                        erSattPåVent = true,
                        "UNDER_BEHANDLING",
                    ),
                    VentestatusHendelse(
                        nå(clock),
                        "saksbehandler",
                        "Begrunnelse 2",
                        erSattPåVent = true,
                        "UNDER_BEHANDLING",
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
                        nå(clock),
                        "saksbehandler",
                        "Begrunnelse",
                        erSattPåVent = true,
                        "UNDER_BEHANDLING",
                    ),
                    VentestatusHendelse(nå(clock), "saksbehandler", "", erSattPåVent = false, "UNDER_BEHANDLING"),
                    VentestatusHendelse(nå(clock), "saksbehandler", "", erSattPåVent = false, "UNDER_BEHANDLING"),
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
                        liktTidspunkt,
                        "saksbehandler",
                        "Begrunnelse",
                        erSattPåVent = true,
                        "UNDER_BEHANDLING",
                    ),
                    VentestatusHendelse(liktTidspunkt, "saksbehandler", "", erSattPåVent = false, "UNDER_BEHANDLING"),
                ),
            )
        }.message shouldBe "Hendelsene må være sortert etter tidspunkt"
    }
}
