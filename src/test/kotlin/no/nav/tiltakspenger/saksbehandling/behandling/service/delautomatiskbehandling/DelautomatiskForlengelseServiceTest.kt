package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
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

    @Test
    fun `deltakelsesmengden er også endret - overlates til saksbehandler`() {
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
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseTilOgMed = 30.juni(2025), antallDagerPerUke = 2F))

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_ENDRET_DELTAKELSESMENGDE)
        }
    }

    @Test
    fun `vedtaket har barnetillegg - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
                barnetillegg = barnetillegg(periode = deltakelse.periode!!, antallBarn = AntallBarn(1)),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_HAR_BARNETILLEGG)
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
