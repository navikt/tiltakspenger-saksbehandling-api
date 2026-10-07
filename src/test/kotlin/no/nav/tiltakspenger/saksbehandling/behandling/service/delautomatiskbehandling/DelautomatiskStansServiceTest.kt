package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.getOrElse
import arrow.core.nonEmptySetOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.AutomatiskStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.KanIkkeStanseAutomatisk
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.AutomatiskRevurderingAvEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.VurdertTiltaksdeltakerEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.finnEndringerForDeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import org.junit.jupiter.api.Test

class DelautomatiskStansServiceTest {

    /**
     * Kjører [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb], som oppretter en stans-revurdering uten saksbehandler.
     * Gir også stansen jobben har vurdert endringen til, med verdiene for å behandle den automatisk.
     */
    private suspend fun TestApplicationContextMedPostgres.opprettAutomatiskStans(
        sak: Sak,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern,
    ): Pair<Revurdering, AutomatiskRevurderingAvEndring.Stans> {
        val stans = sakContext.sakRepo.hentForSakId(sak.id)!!
            .finnEndringerForDeltakelse(tiltaksdeltakelse.internDeltakelseId, nåtilstand.tilLibsDeltakelse(), clock)
            .shouldBeInstanceOf<VurdertTiltaksdeltakerEndring.Endret>()
            .automatiskRevurdering.shouldBeInstanceOf<AutomatiskRevurderingAvEndring.Stans>()
        oppdaterTiltaksdeltakelse(sak.fnr, nåtilstand)
        tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(
            id = tiltaksdeltakelse.internDeltakelseId,
            sakId = sak.id,
            tidspunkt = nå(clock).minusMinutes(20),
        )
        val deltaker = tiltakContext.tiltaksdeltakerRepo
            .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId).shouldNotBeNull()
        oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

        val revurdering = sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>().also {
            it.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>()
            it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            it.saksbehandler.shouldBeNull()
        }
        return revurdering to stans
    }

    private fun TestApplicationContextMedPostgres.hentRevurdering(sak: Sak, revurdering: Revurdering): Revurdering =
        sakContext.sakRepo.hentForSakId(sak.id)!!.hentRammebehandling(revurdering.id).shouldBeInstanceOf<Revurdering>()

    @Test
    fun `avbrutt deltakelse med sluttdato i fortiden - fylles ut og sendes til beslutning av tp-sak`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val (revurdering, stans) = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt, deltakelseTilOgMed = 20.april(2025)),
            )

            val resultat = tac.behandlingContext.delautomatiskStansService.forsøkAutomatiskStans(
                sakId = sak.id,
                revurderingId = revurdering.id,
                stans = stans,
                correlationId = CorrelationId.generate(),
            ).getOrElse { throw AssertionError("Forventet automatisk stans, men fikk $it") }

            listOf(resultat, tac.hentRevurdering(sak, revurdering)).forEach {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                it.beslutter.shouldBeNull()
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
            val (revurdering, stans) = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )
            stans.utfylling.shouldBeNull()

            tac.behandlingContext.delautomatiskStansService.forsøkAutomatiskStans(
                sakId = sak.id,
                revurderingId = revurdering.id,
                stans = stans,
                correlationId = CorrelationId.generate(),
            ).leftOrNull() shouldBe KanIkkeStanseAutomatisk.ManglerUtfylling

            tac.hentRevurdering(sak, revurdering) shouldBe revurdering
        }
    }

    @Test
    fun `manuelt opprettet stans - stanses ikke automatisk`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val (_, revurdering) = startRevurderingStans(tac, sak.id)!!

            tac.behandlingContext.delautomatiskStansService.forsøkAutomatiskStans(
                sakId = sak.id,
                revurderingId = revurdering.id,
                stans = AutomatiskRevurderingAvEndring.Stans(
                    utfylling = AutomatiskStans(
                        hjemmel = HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak,
                        stansFraOgMed = ValgtStansFraOgMed.StansFraOgMed(21.april(2025)),
                        begrunnelse = Begrunnelse.create("begrunnelse")!!,
                    ),
                ),
                correlationId = CorrelationId.generate(),
            ).leftOrNull() shouldBe KanIkkeStanseAutomatisk.ErIkkeAutomatiskOpprettetStans

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe revurdering.status
                it.saksbehandler shouldBe revurdering.saksbehandler
                it.resultat shouldBe Revurderingsresultat.Stans.empty
            }
        }
    }
}
