package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.settPåVent

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.infra.route.rammebehandlingJson
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldHaSisteVentestatus
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgSettPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SettRammebehandlingPåVentRouteTest {
    @Test
    fun `sett søknadsbehandling på vent`() {
        withTestApplicationContext { tac ->
            val (_, _, søknadsbehandling, sakJson) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!
            søknadsbehandling!!.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            søknadsbehandling.saksbehandler shouldBe null
            søknadsbehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "Z12345",
                            begrunnelse = "Begrunnelse for å sette rammebehandling på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = LocalDate.parse("2026-01-01"),
                        ),
                    ),
                ),
            )
            sakJson.rammebehandlingJson(søknadsbehandling.id).also { behandlingJson ->
                behandlingJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
                behandlingJson.get("saksbehandler").isNull shouldBe true
                behandlingJson.shouldHaSisteVentestatus(
                    sattPåVentAv = "Z12345",
                    begrunnelse = "Begrunnelse for å sette rammebehandling på vent",
                    status = "UNDER_BEHANDLING",
                    frist = LocalDate.parse("2026-01-01"),
                    forventetAntallHendelser = 1,
                )
            }
        }
    }
}
