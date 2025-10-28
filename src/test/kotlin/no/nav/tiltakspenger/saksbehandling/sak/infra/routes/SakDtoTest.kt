package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingstypeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class SakDtoTest {

    @Test
    fun `behandlingsoversikt inneholder behandlinger, og søknader uten behandling`() {
        val (sak) = ObjectMother.sakMedOpprettetBehandling()
        val nySøknad = ObjectMother.nyInnvilgbarSøknad()
        // TODO - burde muligens ha en sak.nySøknad()
        val sakMedSøknadOgBehandling = sak.copy(søknader = sak.søknader + nySøknad)

        val actual = sakMedSøknadOgBehandling.toSakDTO(fixedClock)
        actual.behandlingsoversikt.size shouldBe 2
        actual.behandlingsoversikt.first().let {
            it.typeBehandling shouldBe RammebehandlingstypeDTO.SØKNAD
        }
        actual.behandlingsoversikt.last().let {
            it.typeBehandling shouldBe RammebehandlingstypeDTO.SØKNADSBEHANDLING
        }
    }
}
