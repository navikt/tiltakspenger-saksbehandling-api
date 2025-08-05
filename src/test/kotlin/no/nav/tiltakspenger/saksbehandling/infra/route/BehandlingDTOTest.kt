package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BehandlingDTOTest {
    @Nested
    inner class BehandlingSattPåVent {
        @Test
        fun `Den nyeste begrunnelsen blir med`() {
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning()
            val saksbehandler = ObjectMother.saksbehandler()

            val femteAugust = 5.august(2025).atStartOfDay()
            val sjetteAugust = 6.august(2025).atStartOfDay()

            var behandlingSattPåVent = behandling.settPåVent(saksbehandler, "1", femteAugust)
            behandlingSattPåVent = behandlingSattPåVent.gjenoppta(saksbehandler, sjetteAugust)
            behandlingSattPåVent = behandlingSattPåVent.settPåVent(saksbehandler, "2", sjetteAugust)

            behandlingSattPåVent.sattPåVentBegrunnelser.size shouldBe 2
            val dto = behandlingSattPåVent.tilBehandlingDTO()

            dto.erSattPåVent shouldBe true
            dto.sattPåVentBegrunnelse?.sattPåVentAv shouldBe saksbehandler.navIdent
            dto.sattPåVentBegrunnelse?.begrunnelse shouldBe "2"
            dto.sattPåVentBegrunnelse?.tidspunkt shouldBe sjetteAugust.toString()
        }
    }
}
