package no.nav.tiltakspenger.saksbehandling.sak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
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
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
        val sak = ObjectMother.nySak(behandlinger = Behandlinger(behandling), søknader = listOf(behandling.søknad))

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

    @Test
    fun `harSoknadUnderBehandling - har åpen søknad - returnerer true`() {
        val søknad = ObjectMother.nySøknad()
        val sak = ObjectMother.nySak(søknader = listOf(søknad))

        sak.harSoknadUnderBehandling() shouldBe true
    }

    @Test
    fun `harSoknadUnderBehandling - har åpen søknadsbehandling - returnerer true`() {
        val sak = ObjectMother.sakMedOpprettetBehandling().first

        sak.harSoknadUnderBehandling() shouldBe true
    }

    @Test
    fun `harSoknadUnderBehandling - har iverksatt søknadsbehandling - returnerer false`() {
        val sak = ObjectMother.nySakMedVedtak().first

        sak.harSoknadUnderBehandling() shouldBe false
    }

    @Test
    fun `harSoknadUnderBehandling - har iverksatt søknadsbehandling og ny søknad - returnerer true`() {
        val sak = ObjectMother.nySakMedVedtak().first
        val soknad = ObjectMother.nySøknad(fnr = sak.fnr, sakId = sak.id)
        val soknader = sak.soknader
        val oppdatertSak = sak.copy(soknader = soknader + soknad)

        oppdatertSak.harSoknadUnderBehandling() shouldBe true
    }

    @Test
    fun `harSoknadUnderBehandling - har iverksatt søknadsbehandling og ny behandling av samme søknad - returnerer true`() {
        val sak = ObjectMother.nySakMedVedtak().first
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            søknad = sak.soknader.first(),
        )

        val behandlinger = sak.behandlinger
        val oppdatertSak = sak.copy(behandlinger = Behandlinger(behandlinger + behandling))

        oppdatertSak.harSoknadUnderBehandling() shouldBe true
    }
}
