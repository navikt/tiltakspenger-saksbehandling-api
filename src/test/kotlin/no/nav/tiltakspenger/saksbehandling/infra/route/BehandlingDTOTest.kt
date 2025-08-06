package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class BehandlingDTOTest {
    @Nested
    inner class BehandlingSattPåVent {
        val clock: Clock = Clock.fixed(Instant.parse("2025-08-05T12:30:00Z"), ZoneOffset.UTC)

        @Test
        fun `Den nyeste begrunnelsen blir med`() {
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning()
            val saksbehandler = ObjectMother.saksbehandler()

            var behandlingSattPåVent = behandling.settPåVent(saksbehandler, "1", clock)
            behandlingSattPåVent = behandlingSattPåVent.gjenoppta(saksbehandler, clock)
            behandlingSattPåVent = behandlingSattPåVent.settPåVent(saksbehandler, "2", clock)

            behandlingSattPåVent.ventestatus.ventestatusHendelser.size shouldBe 3
            val dto = behandlingSattPåVent.tilBehandlingDTO()

            dto.ventestatus.erSattPåVent shouldBe true
            dto.ventestatus.sattPåVentAv shouldBe saksbehandler.navIdent
            dto.ventestatus.begrunnelse shouldBe "2"
        }
    }
}
