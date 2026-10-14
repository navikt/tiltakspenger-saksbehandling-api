package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.toggleKanSendeHelgForMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.IngenRelevantEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet
import org.junit.jupiter.api.Test

class DelautomatiskForlengelseServiceTest {

    /**
     * Kjører [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb], som oppretter en forlengelse under automatisk behandling.
     */
    private suspend fun TestApplicationContextMedPostgres.opprettAutomatiskForlengelse(
        sak: Sak,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern,
    ): Revurdering {
        behandleEndring(sak, tiltaksdeltakelse, nåtilstand).shouldBeInstanceOf<RevurderingOpprettet>()

        return sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>().also {
            it.resultat shouldBe Revurderingsresultat.Innvilgelse.empty
            it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
            it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
        }
    }

    private suspend fun TestApplicationContextMedPostgres.behandleEndring(
        sak: Sak,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern,
    ): TiltaksdeltakelseEndringBehandlet {
        oppdaterTiltaksdeltakelse(sak.fnr, nåtilstand)
        tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(
            id = tiltaksdeltakelse.internDeltakelseId,
            sakId = sak.id,
            tidspunkt = nå(clock).minusMinutes(20),
        )
        val deltaker = tiltakContext.tiltaksdeltakerRepo
            .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId).shouldNotBeNull()
        return oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker).getOrFail()
    }

    private fun TestApplicationContextMedPostgres.hentRevurdering(sak: Sak, revurdering: Revurdering): Revurdering =
        sakContext.sakRepo.hentForSakId(sak.id)!!.hentRammebehandling(revurdering.id).shouldBeInstanceOf<Revurdering>()

    private fun Revurdering.skalVæreOverlattTilSaksbehandler(grunn: ManueltBehandlesGrunn) {
        status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
        saksbehandler.shouldBeNull()
        manueltBehandlesGrunner shouldBe listOf(grunn)
        resultat shouldBe Revurderingsresultat.Innvilgelse.empty
    }

    @Test
    fun `deltakelsen er forlenget - fylles ut og sendes til beslutning av tp-sak`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                it.beslutter.shouldBeNull()
                it.manueltBehandlesGrunner.shouldBeEmpty()
                it.begrunnelseVilkårsvurdering.shouldNotBeNull()
                it.skalSendeVedtaksbrev shouldBe true
                it.resultat.shouldBeInstanceOf<Revurderingsresultat.Innvilgelse>()
                it.innvilgelsesperioder.shouldNotBeNull().totalPeriode shouldBe (6.mai(2025) til 30.juni(2025))
                it.antallDagerPerMeldeperiode.shouldNotBeNull().verdier.distinct() shouldBe
                    vedtak.antallDagerPerMeldeperiode.shouldNotBeNull().verdier.distinct()
                it.barnetillegg.shouldNotBeNull().periodisering.verdier.distinct() shouldBe listOf(AntallBarn.ZERO)
            }
        }
    }

    /**
     * Iverksetter en innvilgelse av deltakelsen, oppretter en forlengelse til 30. juni og registrerer [nåtilstand] før forlengelsen behandles.
     * Gir forlengelsen slik den er lagret etter den automatiske behandlingen.
     */
    private suspend fun ApplicationTestBuilder.forlengOgBehandleAutomatisk(
        tac: TestApplicationContextMedPostgres,
        nåtilstand: (TiltaksdeltakelseIntern) -> TiltaksdeltakelseIntern = { it },
        antallBarn: AntallBarn = AntallBarn.ZERO,
        kanSendeHelg: Boolean = false,
    ): Revurdering {
        val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
        val (sak) = iverksettSøknadsbehandling(
            tac = tac,
            tiltaksdeltakelse = deltakelse,
            innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            barnetillegg = barnetillegg(periode = deltakelse.periode!!, antallBarn = antallBarn),
        )
        if (kanSendeHelg) {
            toggleKanSendeHelgForMeldekort(tac, sak.id, kanSendeHelg = true)
        }
        val forlenget = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025))
        val revurdering = tac.opprettAutomatiskForlengelse(sak = sak, tiltaksdeltakelse = deltakelse, nåtilstand = forlenget)
        tac.oppdaterTiltaksdeltakelse(sak.fnr, nåtilstand(forlenget))

        tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

        return tac.hentRevurdering(sak, revurdering)
    }

    private fun Revurdering.skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode: Int, antallBarn: AntallBarn = AntallBarn.ZERO) {
        status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        manueltBehandlesGrunner.shouldBeEmpty()
        innvilgelsesperioder.shouldNotBeNull().totalPeriode shouldBe (6.mai(2025) til 30.juni(2025))
        this.antallDagerPerMeldeperiode.shouldNotBeNull().verdier.distinct() shouldBe listOf(AntallDagerForMeldeperiode(antallDagerPerMeldeperiode))
        barnetillegg.shouldNotBeNull().periodisering.also {
            it.totalPeriode shouldBe (6.mai(2025) til 30.juni(2025))
            it.verdier.distinct() shouldBe listOf(antallBarn)
        }
    }

    @Test
    fun `deltakelsesmengden er endret til dager per uke - antall dager utledes fra dager per uke`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(antallDagerPerUke = 2.5F, deltakelseProsent = 50F) }).also {
                it.skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 5)
                it.begrunnelseVilkårsvurdering.shouldNotBeNull().verdi shouldContain
                    "Deltakelsesmengden er endret til 2,5 dager per uke, som gir 5 dager per meldeperiode."
            }
        }
    }

    @Test
    fun `deltakelsesmengden er endret til prosent uten dager per uke - antall dager utledes fra prosenten`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(antallDagerPerUke = null, deltakelseProsent = 80F) }).also {
                it.skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 8)
                it.begrunnelseVilkårsvurdering.shouldNotBeNull().verdi shouldContain
                    "Deltakelsesmengden er endret til 80 prosent, som gir 8 dager per meldeperiode."
            }
        }
    }

    @Test
    fun `deltakelsesmengden er endret til mer enn fulltid - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(antallDagerPerUke = 6F) })
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_DELTAKELSESMENGDE)
        }
    }

    @Test
    fun `deltakelsesmengden er endret og bruker kan melde i helg - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(antallDagerPerUke = 2F) }, kanSendeHelg = true)
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_ENDRET_DELTAKELSESMENGDE_OG_KAN_MELDE_I_HELG)
        }
    }

    @Test
    fun `bruker kan melde i helg med uendret deltakelsesmengde - fylles ut og sendes til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, kanSendeHelg = true)
                .skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 10)
        }
    }

    @Test
    fun `vedtaket har barnetillegg - barnetillegget videreføres`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, antallBarn = AntallBarn(2)).also {
                it.skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 10, antallBarn = AntallBarn(2))
                it.begrunnelseVilkårsvurdering.shouldNotBeNull().verdi shouldContain
                    "Barnetillegg for 2 barn videreføres fra det gjeldende vedtaket."
            }
        }
    }

    @Test
    fun `vedtaket er stanset før forlengelsen behandles - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )
            iverksettRevurderingStans(tac = tac, sakId = sak.id, stansFraOgMed = 2.mai(2025))

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_VEDTAKET_ER_STANSET_ELLER_OPPHORT)
        }
    }

    /**
     * Endringer som kommer før forlengelsen er behandlet, fanges opp når forlengelsen henter nå-tilstanden på nytt.
     * Etterpå sammenlignes de med forlengelsen, først som åpen behandling og så som gjeldende vedtak.
     * Ingen av dem skal gi nye revurderinger eller oppgaver.
     */
    @Test
    fun `flere forlengelser før og etter automatisk forlengelse gir verken ny revurdering eller oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )
            val forlengetIgjen = deltakelse.copy(deltakelseTilOgMed = 31.juli(2025))

            tac.behandleEndring(sak, deltakelse, forlengetIgjen).shouldBeInstanceOf<IngenRelevantEndring>()

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.innvilgelsesperioder.shouldNotBeNull().totalPeriode shouldBe (6.mai(2025) til 31.juli(2025))
            }
            tac.behandleEndring(sak, deltakelse, forlengetIgjen).shouldBeInstanceOf<IngenRelevantEndring>()

            taRammebehandlinger(tac, listOf(sak.id to revurdering.id), saksbehandler = ObjectMother.beslutter()).shouldNotBeNull()
            iverksettForBehandlingId(tac, sak.id, revurdering.id).shouldNotBeNull()

            tac.behandleEndring(sak, deltakelse, forlengetIgjen).shouldBeInstanceOf<IngenRelevantEndring>()

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe
                listOf(sak.rammebehandlinger.single().id, revurdering.id)
            tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>().opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
        }
    }

    @Test
    fun `annen åpen behandling på saken - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )
            startRevurderingInnvilgelse(tac, sak.id).shouldNotBeNull()

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.ANNET_APEN_BEHANDLING)
        }
    }

    @Test
    fun `manuelt opprettet innvilgelse - behandles ikke automatisk`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val (_, revurdering) = startRevurderingInnvilgelse(tac, sak.id)!!

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering) shouldBe revurdering
        }
    }
}
