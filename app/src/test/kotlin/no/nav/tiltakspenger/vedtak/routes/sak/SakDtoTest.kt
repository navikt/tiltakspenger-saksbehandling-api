package no.nav.tiltakspenger.vedtak.routes.sak

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.BehandlingstypeDTO
import org.junit.jupiter.api.Test

class SakDtoTest {

    @Test
    fun `behandlingsoversikt inneholder behandlinger, og søknader uten behandling`() {
        val sak = ObjectMother.sakMedOpprettetBehandling()
        val nySøknad = ObjectMother.nySøknad()
        // TODO - burde muligens ha en sak.nySøknad()
        val sakMedSøknadOgBehandling = sak.copy(soknader = sak.soknader + nySøknad)

        val actual = sakMedSøknadOgBehandling.toDTO()
        actual.behandlingsoversikt.size shouldBe 2
        actual.behandlingsoversikt.first().let {
            it.typeBehandling shouldBe BehandlingstypeDTO.FØRSTEGANGSBEHANDLING
        }
        actual.behandlingsoversikt.last().let {
            it.typeBehandling shouldBe BehandlingstypeDTO.SØKNAD
        }
    }
}
