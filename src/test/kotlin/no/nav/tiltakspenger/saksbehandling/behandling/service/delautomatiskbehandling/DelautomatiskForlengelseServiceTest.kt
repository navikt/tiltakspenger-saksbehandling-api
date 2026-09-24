package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse.KanIkkeForlengeAutomatisk
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import org.junit.jupiter.api.Test

class DelautomatiskForlengelseServiceTest {

    private suspend fun TestApplicationContextMedPostgres.opprettForlengelseViaJobben(
        sak: Sak,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern,
    ): Revurdering {
        oppdaterTiltaksdeltakelse(sak.fnr, nåtilstand)
        tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(
            id = tiltaksdeltakelse.internDeltakelseId,
            sakId = sak.id,
            tidspunkt = nå(clock).minusMinutes(20),
        )
        val deltaker = tiltakContext.tiltaksdeltakerRepo
            .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId).shouldNotBeNull()
        oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

        val revurdering = sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
        revurdering.resultat.shouldBeInstanceOf<Revurderingsresultat.Innvilgelse>()
        revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
        return revurdering
    }

    private suspend fun TestApplicationContextMedPostgres.forsøkAutomatiskForlengelse(
        revurdering: Revurdering,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
    ) = behandlingContext.delautomatiskForlengelseService.forsøkAutomatiskForlengelse(
        sakId = revurdering.sakId,
        revurderingId = revurdering.id,
        tiltaksdeltakerId = tiltaksdeltakelse.internDeltakelseId,
        correlationId = CorrelationId.generate(),
    )

    private fun TestApplicationContextMedPostgres.hentRevurdering(revurdering: Revurdering): Revurdering =
        sakContext.sakRepo.hentForSakId(revurdering.sakId)!!.hentRammebehandling(revurdering.id).shouldBeInstanceOf<Revurdering>()

    private fun TestApplicationContextMedPostgres.skalVæreUrørt(revurdering: Revurdering) {
        val lagret = hentRevurdering(revurdering)
        lagret.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
        lagret.saksbehandler.shouldBeNull()
        lagret.innvilgelsesperioder.shouldBeNull()
    }

    @Test
    fun `forlenger automatisk og sender til beslutning når deltakelsen er utvidet fremover i tid`() {
        withTestApplicationContextAndPostgres { tac ->
            val tiltaksdeltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
            )
            val revurdering = tac.opprettForlengelseViaJobben(
                sak = sak,
                tiltaksdeltakelse = tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )

            val resultat = tac.forsøkAutomatiskForlengelse(revurdering, tiltaksdeltakelse)

            resultat.isRight() shouldBe true
            val lagret = tac.hentRevurdering(revurdering)
            lagret.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            lagret.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER.navIdent
            lagret.innvilgelsesperioder.shouldNotBeNull().totalPeriode shouldBe (6.mai(2025) til 30.juni(2025))
            lagret.antallDagerPerMeldeperiode.shouldNotBeNull().verdier.distinct() shouldBe
                vedtak.antallDagerPerMeldeperiode.shouldNotBeNull().verdier.distinct()
            lagret.barnetillegg.shouldNotBeNull().periodisering.verdier.distinct() shouldBe listOf(AntallBarn.ZERO)
        }
    }

    @Test
    fun `forlenger ikke automatisk når vedtaket er stanset`() {
        withTestApplicationContextAndPostgres { tac ->
            val tiltaksdeltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
            )
            iverksettRevurderingStans(tac = tac, sakId = sak.id, stansFraOgMed = 2.mai(2025))
            val revurdering = tac.opprettForlengelseViaJobben(
                sak = sak,
                tiltaksdeltakelse = tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(deltakelseTilOgMed = 5.juni(2025)),
            )

            tac.forsøkAutomatiskForlengelse(revurdering, tiltaksdeltakelse).leftOrNull() shouldBe
                KanIkkeForlengeAutomatisk.VedtaketErStansetEllerOpphørt
            tac.skalVæreUrørt(revurdering)
        }
    }

    @Test
    fun `forlenger ikke automatisk når vedtaket er opphørt`() {
        withTestApplicationContextAndPostgres { tac ->
            val tiltaksdeltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
            )
            iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = vedtak.id,
                vedtaksperiode = 2.mai(2025) til 5.mai(2025),
            )
            val revurdering = tac.opprettForlengelseViaJobben(
                sak = sak,
                tiltaksdeltakelse = tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(deltakelseTilOgMed = 5.juni(2025)),
            )

            tac.forsøkAutomatiskForlengelse(revurdering, tiltaksdeltakelse).leftOrNull() shouldBe
                KanIkkeForlengeAutomatisk.VedtaketErStansetEllerOpphørt
            tac.skalVæreUrørt(revurdering)
        }
    }

    @Test
    fun `forlenger ikke automatisk når også deltakelsesmengden er endret`() {
        withTestApplicationContextAndPostgres { tac ->
            val tiltaksdeltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
            )
            val revurdering = tac.opprettForlengelseViaJobben(
                sak = sak,
                tiltaksdeltakelse = tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(deltakelseTilOgMed = 30.juni(2025), antallDagerPerUke = 2F),
            )

            tac.forsøkAutomatiskForlengelse(revurdering, tiltaksdeltakelse).leftOrNull() shouldBe
                KanIkkeForlengeAutomatisk.ErIkkeEnRenForlengelse
            tac.skalVæreUrørt(revurdering)
        }
    }

    @Test
    fun `forlenger ikke automatisk når vedtaket har barnetillegg`() {
        withTestApplicationContextAndPostgres { tac ->
            val tiltaksdeltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                barnetillegg = barnetillegg(periode = tiltaksdeltakelse.periode!!, antallBarn = AntallBarn(1)),
            )
            val revurdering = tac.opprettForlengelseViaJobben(
                sak = sak,
                tiltaksdeltakelse = tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )

            tac.forsøkAutomatiskForlengelse(revurdering, tiltaksdeltakelse).leftOrNull() shouldBe
                KanIkkeForlengeAutomatisk.HarBarnetillegg
            tac.skalVæreUrørt(revurdering)
        }
    }

    @Test
    fun `forlenger ikke en manuelt opprettet revurdering`() {
        withTestApplicationContextAndPostgres { tac ->
            val tiltaksdeltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
            )
            val (_, revurdering) = startRevurderingStans(tac = tac, sakId = sak.id).shouldNotBeNull()

            tac.forsøkAutomatiskForlengelse(revurdering, tiltaksdeltakelse).leftOrNull() shouldBe
                KanIkkeForlengeAutomatisk.ErIkkeAutomatiskOpprettetInnvilgelse
        }
    }
}
