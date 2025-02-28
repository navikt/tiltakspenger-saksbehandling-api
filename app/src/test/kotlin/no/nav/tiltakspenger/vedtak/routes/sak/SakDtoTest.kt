package no.nav.tiltakspenger.vedtak.routes.sak

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class SakDtoTest {

    @Test
    fun `behandlingsoversikt inneholder behandlinger, og søknader uten behandling`() {
        val sak = ObjectMother.sakMedOpprettetBehandling()
        val nySøknad = ObjectMother.nySøknad()
        // TODO - burde muligens ha en sak.nySøknad()
        val sakMedSøknadOgBehandling = sak.copy(soknader = sak.soknader + nySøknad)

        sakMedSøknadOgBehandling.toDTO().behandlingsoversikt.size shouldBe 2
    }
}
