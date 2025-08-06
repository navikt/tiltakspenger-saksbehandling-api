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
            val beslutter = ObjectMother.beslutter(navIdent = "Z111111")
            val behandling =
                ObjectMother.nySøknadsbehandlingUnderBeslutning(beslutter = beslutter)

            var behandlingSattPåVent = behandling.settPåVent(beslutter, "1", clock)
            behandlingSattPåVent = behandlingSattPåVent.gjenoppta(beslutter, clock)
            behandlingSattPåVent = behandlingSattPåVent.settPåVent(beslutter, "2", clock)

            behandlingSattPåVent.ventestatus.ventestatusHendelser.size shouldBe 3
            val dto = behandlingSattPåVent.tilBehandlingDTO()

            dto.ventestatus?.erSattPåVent shouldBe true
            dto.ventestatus?.sattPåVentAv shouldBe beslutter.navIdent
            dto.ventestatus?.begrunnelse shouldBe "2"
        }
    }
}
