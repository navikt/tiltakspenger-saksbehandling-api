package no.nav.tiltakspenger.saksbehandling.felles

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.nå
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
            .settPåVent(
                tidspunktSattPåVent = nå(clock),
                endretAv = "saksbehandler",
                begrunnelse = "Venter på dokumentasjon",
                status = "UNDER_BEHANDLING",
                frist = LocalDate.now(clock).plusWeeks(1),
            )

        ventestatus.erSattPåVent shouldBe true
        ventestatus.ventestatusHendelser.size shouldBe 1
    }

    @Test
    fun `kaster exception dersom frist er i fortid`() {
        val clock = TikkendeKlokke()
        val ventestatus = Ventestatus()
        shouldThrow<IllegalArgumentException> {
            ventestatus.settPåVent(
                tidspunktSattPåVent = nå(clock),
                endretAv = "saksbehandler",
                begrunnelse = "Venter på dokumentasjon",
                status = "UNDER_BEHANDLING",
                frist = LocalDate.now(clock).minusDays(1),
            )
        }
    }

    @Test
    fun `kan sette på vent og gjenoppta`() {
        val clock = TikkendeKlokke()
        val ventestatus = Ventestatus()
            .settPåVent(
                tidspunktSattPåVent = nå(clock),
                endretAv = "saksbehandler",
                begrunnelse = "Venter på dokumentasjon",
                status = "UNDER_BEHANDLING",
                frist = LocalDate.now(clock).plusWeeks(1),
            )
            .gjenoppta(nå(clock), "saksbehandler", "UNDER_BEHANDLING")

        ventestatus.erSattPåVent shouldBe false
        ventestatus.ventestatusHendelser.size shouldBe 2
    }

    @Test
    fun `kan sette på vent, gjenoppta og sette på vent igjen`() {
        val clock = TikkendeKlokke()
        val ventestatus = Ventestatus()
            .settPåVent(
                tidspunktSattPåVent = nå(clock = clock),
                endretAv = "saksbehandler",
                begrunnelse = "Venter på dokumentasjon",
                status = "UNDER_BEHANDLING",
                frist = null,
            )
            .gjenoppta(nå(clock), "saksbehandler", "UNDER_BEHANDLING")
            .settPåVent(
                tidspunktSattPåVent = nå(clock),
                endretAv = "saksbehandler",
                begrunnelse = "Venter på mer info",
                status = "UNDER_BEHANDLING",
                frist = LocalDate.now(clock).plusWeeks(1),
            )

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
        }.message shouldBe "Første ventestatus må være satt på vent"
    }

    @Test
    fun `kan sette på vent to ganger på rad, og siste hendelse gir gjeldende begrunnelse og frist`() {
        val clock = TikkendeKlokke()
        val oppdatertFrist = LocalDate.now(clock).plusWeeks(2)
        val ventestatus = Ventestatus()
            .settPåVent(
                tidspunktSattPåVent = nå(clock),
                endretAv = "saksbehandler",
                begrunnelse = "Begrunnelse 1",
                status = "KLAR_TIL_BEHANDLING",
                frist = LocalDate.now(clock).plusWeeks(1),
            )
            .settPåVent(
                tidspunktSattPåVent = nå(clock),
                endretAv = "saksbehandler",
                begrunnelse = "Begrunnelse 2",
                status = "KLAR_TIL_BEHANDLING",
                frist = oppdatertFrist,
            )

        ventestatus.erSattPåVent shouldBe true
        ventestatus.ventestatusHendelser.size shouldBe 2
        ventestatus.sattPåVentBegrunnelse shouldBe "Begrunnelse 2"
        ventestatus.sattPåVentFrist shouldBe oppdatertFrist
    }

    @Test
    fun `kan ikke gjenoppta to ganger på rad`() {
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
        }.message shouldBe "To hendelser på rad kan ikke være gjenopptatt"
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
