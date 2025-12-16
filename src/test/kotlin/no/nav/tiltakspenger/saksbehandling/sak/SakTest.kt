package no.nav.tiltakspenger.saksbehandling.sak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.enUkeEtterFixedClock
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtaksliste
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjortAvRammevedtak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SakTest {
    @Test
    fun `avbryter søknad`() {
        val søknad = ObjectMother.nyInnvilgbarSøknad()
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
        sakMedAvbruttsøknad.søknader.size shouldBe 1
        sakMedAvbruttsøknad.rammebehandlinger.size shouldBe 0
    }

    @Test
    fun `avbryter behandling`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
        val sak = ObjectMother.nySak(behandlinger = Rammebehandlinger(behandling), søknader = listOf(behandling.søknad))

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
        sakMedAvbruttsøknad.søknader.size shouldBe 1
        sakMedAvbruttsøknad.rammebehandlinger.size shouldBe 1
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
        val søknad = ObjectMother.nyInnvilgbarSøknad()
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
        val soknad = ObjectMother.nyInnvilgbarSøknad(fnr = sak.fnr, sakId = sak.id)
        val soknader = sak.søknader
        val oppdatertSak = sak.copy(søknader = soknader + soknad)

        oppdatertSak.harSoknadUnderBehandling() shouldBe true
    }

    @Test
    fun `harSoknadUnderBehandling - har iverksatt søknadsbehandling og ny behandling av samme søknad - returnerer true`() {
        val clock = TikkendeKlokke()
        val sak = ObjectMother.nySakMedVedtak(
            clock = clock,
        ).first
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            søknad = sak.søknader.filterIsInstance<InnvilgbarSøknad>().first(),
            clock = clock,
        )
        val oppdatertSak = sak.leggTilSøknadsbehandling(behandling)

        oppdatertSak.harSoknadUnderBehandling() shouldBe true
    }

    @Test
    fun `henter de nyeste tiltaksdeltakelsene basert på rammevedtak`() {
        val deltakelsesId = "deltakelses-id"
        val sakId = SakId.random()
        val fnr = Fnr.random()

        val vedtaksperiode = Periode(1.april(2025), 30.april(2025))
        val v1 = Rammevedtak(
            id = VedtakId.random(),
            opprettet = LocalDateTime.now(fixedClock),
            sakId = sakId,
            periode = vedtaksperiode,
            journalpostId = null,
            journalføringstidspunkt = null,
            utbetaling = null,
            behandling = ObjectMother.nyVedtattSøknadsbehandling(
                sakId = sakId,
                fnr = fnr,
                virkningsperiode = vedtaksperiode,
                innvilgelsesperioder = listOf(
                    innvilgelsesperiodeKommando(
                        periode = vedtaksperiode,
                        tiltaksdeltakelseId = deltakelsesId,
                    ),
                ),
                saksopplysninger = ObjectMother.saksopplysninger(
                    fom = vedtaksperiode.fraOgMed,
                    tom = vedtaksperiode.tilOgMed,
                    tiltaksdeltakelse = listOf(
                        ObjectMother.tiltaksdeltakelse(
                            eksternTiltaksdeltakelseId = deltakelsesId,
                            fom = vedtaksperiode.fraOgMed,
                            tom = vedtaksperiode.tilOgMed,
                        ),
                    ),
                ),
            ),
            vedtaksdato = null,
            distribusjonId = null,
            distribusjonstidspunkt = null,
            sendtTilDatadeling = null,
            brevJson = "",
            omgjortAvRammevedtak = OmgjortAvRammevedtak(emptyList()),
        )

        val andreVedtaksPeriode = Periode(1.mai(2025), 31.mai(2025))
        val v2 = Rammevedtak(
            id = VedtakId.random(),
            opprettet = LocalDateTime.now(enUkeEtterFixedClock),
            sakId = sakId,
            periode = andreVedtaksPeriode,
            journalpostId = null,
            journalføringstidspunkt = null,
            utbetaling = null,
            behandling = ObjectMother.nyVedtattRevurderingInnvilgelse(
                sakId = sakId,
                fnr = fnr,
                virkningsperiode = andreVedtaksPeriode,
                innvilgelsesperioder = listOf(
                    innvilgelsesperiodeKommando(
                        periode = andreVedtaksPeriode,
                        tiltaksdeltakelseId = deltakelsesId,
                    ),
                ),
                saksopplysninger = ObjectMother.saksopplysninger(
                    fom = andreVedtaksPeriode.fraOgMed,
                    tom = andreVedtaksPeriode.tilOgMed,
                    tiltaksdeltakelse = listOf(
                        ObjectMother.tiltaksdeltakelse(
                            eksternTiltaksdeltakelseId = deltakelsesId,
                            fom = andreVedtaksPeriode.fraOgMed,
                            tom = andreVedtaksPeriode.tilOgMed,
                        ),
                    ),
                ),
            ),
            vedtaksdato = null,
            distribusjonId = null,
            distribusjonstidspunkt = null,
            sendtTilDatadeling = null,
            brevJson = "",
            omgjortAvRammevedtak = OmgjortAvRammevedtak(emptyList()),
        )

        val sak = Sak(
            id = sakId,
            fnr = fnr,
            saksnummer = Saksnummer.genererSaknummer(1.desember(2025), "0001"),
            behandlinger = Behandlinger(
                Rammebehandlinger(emptyList()),
                Meldekortbehandlinger(emptyList()),
            ),
            vedtaksliste = Vedtaksliste(
                Rammevedtaksliste(listOf(v1, v2)),
                Meldekortvedtaksliste(emptyList()),
            ),
            meldeperiodeKjeder = MeldeperiodeKjeder(),
            brukersMeldekort = listOf(),
            søknader = listOf(),
            kanSendeInnHelgForMeldekort = false,
        )

        sak.hentNyesteTiltaksdeltakelserForRammevedtakIder(
            listOf(v1.id, v2.id),
        ) shouldBe Tiltaksdeltakelser(
            v2.valgteTiltaksdeltakelser!!.verdier.single(),
        )
    }
}
