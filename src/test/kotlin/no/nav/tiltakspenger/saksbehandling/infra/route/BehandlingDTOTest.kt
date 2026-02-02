package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
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
            runTest {
                val correlationId = CorrelationId.generate()
                val beslutter = ObjectMother.beslutter(navIdent = "Z111111")
                val behandling =
                    ObjectMother.nySøknadsbehandlingUnderBeslutning(beslutter = beslutter)

                val behandlingSattPåVent = behandling
                    .settPåVent(beslutter, "1", clock)
                    .gjenoppta(beslutter, correlationId, clock) { behandling.saksopplysninger }.getOrFail()
                    .settPåVent(beslutter, "2", clock)

                behandlingSattPåVent.ventestatus.ventestatusHendelser.size shouldBe 3
                behandlingSattPåVent.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                behandlingSattPåVent.saksbehandler shouldBe behandling.saksbehandler
                behandlingSattPåVent.beslutter shouldBe null

                val sak = ObjectMother.nySak(behandlinger = Rammebehandlinger(listOf(behandlingSattPåVent)))

                val dto = sak.tilRammebehandlingDTO(behandlingSattPåVent.id)

                dto.ventestatus?.erSattPåVent shouldBe true
                dto.ventestatus?.sattPåVentAv shouldBe beslutter.navIdent
                dto.ventestatus?.begrunnelse shouldBe "2"
                dto.beslutter shouldBe null
            }
        }
    }
}
