package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingstypeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class SakDtoTest {

    @Test
    fun `behandlingsoversikt inneholder behandlinger, og søknader uten behandling`() {
        val (sak) = ObjectMother.sakMedOpprettetBehandling()
        val nySøknad = ObjectMother.nySøknad()
        // TODO - burde muligens ha en sak.nySøknad()
        val sakMedSøknadOgBehandling = sak.copy(soknader = sak.soknader + nySøknad)

        val actual = sakMedSøknadOgBehandling.toSakDTO(fixedClock)
        actual.behandlingsoversikt.size shouldBe 2
        actual.behandlingsoversikt.first().let {
            it.typeBehandling shouldBe BehandlingstypeDTO.SØKNAD
        }
        actual.behandlingsoversikt.last().let {
            it.typeBehandling shouldBe BehandlingstypeDTO.FØRSTEGANGSBEHANDLING
        }
    }
}
