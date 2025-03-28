package no.nav.tiltakspenger.saksbehandling.domene.sak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SakTest {
    @Test
    fun `avbryter søknad`() {
        val søknad = ObjectMother.nySøknad()
        val sak = ObjectMother.nySak(søknader = listOf(søknad))

        val (sakMedAvbruttsøknad, avbruttSøknad, behandling) = sak.avbrytSøknadOgBehandling(
            no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingCommand(
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
            no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingCommand(
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

    @Nested
    inner class GenerererMeldeperioder {
        @Test
        fun `for en ny sak som er tom`() {
            val sak = ObjectMother.nySak()
            val actual = sak.genererMeldeperioder(fixedClock)

            actual.let {
                it.first.meldeperiodeKjeder.size shouldBe 0
                it.second.size shouldBe 0
            }
        }

        @Test
        fun `for en sak med et vedtak`() {
            val virkningsperiode = Periode(9.april(2024), 16.april(2024))
            val (sak) = ObjectMother.nySakMedVedtak(virkningsperiode = virkningsperiode)
            val (sakMedMeldeperioder, meldeperioder) = sak.genererMeldeperioder(fixedClock)

            sakMedMeldeperioder.let {
                it.meldeperiodeKjeder.single() shouldBe meldeperioder
                meldeperioder.size shouldBe 1
            }

            val (sakDerViPrøverÅGenerePåNytt, nyeMeldeperioder) = sakMedMeldeperioder.genererMeldeperioder(fixedClock)

            sakMedMeldeperioder shouldBe sakDerViPrøverÅGenerePåNytt
            nyeMeldeperioder.size shouldBe 0
        }
    }
}
