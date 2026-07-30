package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class SakDtoTest {

    @Test
    fun `behandlingsoversikt inneholder behandlinger, og søknader uten behandling`() {
        val (sak) = ObjectMother.sakMedOpprettetBehandling()
        val nySøknad = ObjectMother.nyInnvilgbarSøknad()
        val sakMedSøknadOgBehandling = sak.leggTilSøknad(nySøknad)

        val actual = sakMedSøknadOgBehandling.toSakDTO(ObjectMother.saksbehandler(), fixedClock)
        actual.åpneBehandlinger.size shouldBe 2
        actual.åpneBehandlinger.first().type shouldBe ÅpenBehandlingTypeDTO.SØKNADSBEHANDLING
        actual.åpneBehandlinger.last().type shouldBe ÅpenBehandlingTypeDTO.SØKNAD
        actual.åpneBehandlinger.last().id shouldBe nySøknad.id.toString()
    }
}
