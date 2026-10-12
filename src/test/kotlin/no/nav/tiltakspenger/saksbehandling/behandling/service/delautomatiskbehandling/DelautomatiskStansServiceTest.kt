package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.nonEmptySetOf
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.IngenRelevantEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet
import org.junit.jupiter.api.Test

class DelautomatiskStansServiceTest {

    /**
     * Kjører [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb], som oppretter en stans under automatisk behandling.
     */
    private suspend fun TestApplicationContextMedPostgres.opprettAutomatiskStans(
        sak: Sak,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern,
    ): Revurdering {
        behandleEndring(sak, tiltaksdeltakelse, nåtilstand).shouldBeInstanceOf<RevurderingOpprettet>()

        return sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>().also {
            it.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>()
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
        resultat shouldBe Revurderingsresultat.Stans.empty
    }

    @Test
    fun `avbrutt deltakelse med sluttdato i fortiden - fylles ut og sendes til beslutning av tp-sak`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt, deltakelseTilOgMed = 20.april(2025)),
            )

            tac.delautomatiskStansJobb.automatiskBehandleStanser()

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                it.beslutter.shouldBeNull()
                it.manueltBehandlesGrunner.shouldBeEmpty()
                it.begrunnelseVilkårsvurdering.shouldNotBeNull()
                it.skalSendeVedtaksbrev shouldBe true
                val stans = it.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>()
                stans.valgtHjemmel shouldBe nonEmptySetOf(HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak)
                stans.harValgtStansFraFørsteDagSomGirRett shouldBe false
                stans.stansperiode shouldBe (21.april(2025) til 5.mai(2025))
            }
        }
    }

    @Test
    fun `avbrutt deltakelse med sluttdato i fremtiden - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )

            tac.delautomatiskStansJobb.automatiskBehandleStanser()

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.STANS_SLUTTDATO_ER_IKKE_PASSERT)
        }
    }

    @Test
    fun `deltakelsen er tatt opp igjen før stansen behandles - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt, deltakelseTilOgMed = 20.april(2025)),
            )
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse)

            tac.delautomatiskStansJobb.automatiskBehandleStanser()

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.STANS_DELTAKELSEN_ER_IKKE_AVSLUTTET)
        }
    }

    /**
     * Endringer som kommer før stansen er behandlet, fanges opp når stansen henter nå-tilstanden på nytt.
     * Etterpå sammenlignes de med stansen, først som åpen behandling og så som gjeldende vedtak.
     * Ingen av dem skal gi nye revurderinger eller oppgaver.
     */
    @Test
    fun `flere endringer før og etter automatisk stans gir verken ny revurdering eller oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val avbrutt = deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt, deltakelseTilOgMed = 20.april(2025))
            val revurdering = tac.opprettAutomatiskStans(sak = sak, tiltaksdeltakelse = deltakelse, nåtilstand = avbrutt)
            val avbruttTidligere = avbrutt.copy(deltakelseTilOgMed = 15.april(2025))

            tac.behandleEndring(sak, deltakelse, avbruttTidligere).shouldBeInstanceOf<IngenRelevantEndring>()

            tac.delautomatiskStansJobb.automatiskBehandleStanser()

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>().stansperiode shouldBe (16.april(2025) til 5.mai(2025))
            }
            tac.behandleEndring(sak, deltakelse, avbruttTidligere).shouldBeInstanceOf<IngenRelevantEndring>()

            taRammebehandlinger(tac, listOf(sak.id to revurdering.id), saksbehandler = ObjectMother.beslutter()).shouldNotBeNull()
            iverksettForBehandlingId(tac, sak.id, revurdering.id).shouldNotBeNull()

            tac.behandleEndring(sak, deltakelse, avbruttTidligere).shouldBeInstanceOf<IngenRelevantEndring>()
            tac.behandleEndring(sak, deltakelse, avbrutt).shouldBeInstanceOf<IngenRelevantEndring>()

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
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt, deltakelseTilOgMed = 20.april(2025)),
            )
            startRevurderingInnvilgelse(tac, sak.id).shouldNotBeNull()

            tac.delautomatiskStansJobb.automatiskBehandleStanser()

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.ANNET_APEN_BEHANDLING)
        }
    }

    @Test
    fun `manuelt opprettet stans - behandles ikke automatisk`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val (_, revurdering) = startRevurderingStans(tac, sak.id)!!

            tac.delautomatiskStansJobb.automatiskBehandleStanser()
            tac.behandlingContext.delautomatiskStansService.forsøkAutomatiskStans(
                sakId = sak.id,
                revurderingId = revurdering.id,
                correlationId = CorrelationId.generate(),
            )

            tac.hentRevurdering(sak, revurdering) shouldBe revurdering
        }
    }
}
