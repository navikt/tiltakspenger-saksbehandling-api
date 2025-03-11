package no.nav.tiltakspenger.saksbehandling.domene.sak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.avslutt.AvbrytSøknadOgBehandlingCommand
import org.junit.jupiter.api.Test

class SakTest {
    @Test
    fun `avbryter søknad`() {
        val søknad = ObjectMother.nySøknad()
        val sak = ObjectMother.nySak(søknader = listOf(søknad))

        val (sakMedAvbruttsøknad, avbruttSøknad, behandling) = sak.avbrytSøknadOgBehandling(
            AvbrytSøknadOgBehandlingCommand(
                saksnummer = sak.saksnummer,
                søknadId = søknad.id,
                behandlingId = null,
                avsluttetAv = ObjectMother.saksbehandler(),
                correlationId = CorrelationId.generate(),
                begrunnelse = "begrunnelse",
            ),
            avbruttTidspunkt = førsteNovember24,
        )

        avbruttSøknad?.avbrutt shouldNotBe null
        behandling shouldBe null
        sakMedAvbruttsøknad.soknader.size shouldBe 1
        sakMedAvbruttsøknad.behandlinger.size shouldBe 0
    }

    @Test
    fun `avbryter behandling`() {
        val behandling = ObjectMother.nyBehandling()
        val sak = ObjectMother.nySak(behandlinger = Behandlinger(behandling), søknader = listOf(behandling.søknad!!))

        val (sakMedAvbruttsøknad, avbruttSøknad, avbruttBehandling) = sak.avbrytSøknadOgBehandling(
            AvbrytSøknadOgBehandlingCommand(
                saksnummer = sak.saksnummer,
                søknadId = null,
                behandlingId = behandling.id,
                avsluttetAv = ObjectMother.saksbehandler(),
                correlationId = CorrelationId.generate(),
                begrunnelse = "begrunnelse",
            ),
            avbruttTidspunkt = førsteNovember24,
        )
        avbruttSøknad?.avbrutt shouldNotBe null
        avbruttBehandling?.avbrutt shouldNotBe null
        sakMedAvbruttsøknad.soknader.size shouldBe 1
        sakMedAvbruttsøknad.behandlinger.size shouldBe 1
    }
}
