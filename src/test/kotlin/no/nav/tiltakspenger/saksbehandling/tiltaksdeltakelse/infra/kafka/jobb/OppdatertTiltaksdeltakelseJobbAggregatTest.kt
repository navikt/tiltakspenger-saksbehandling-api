package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import arrow.core.Either
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.KunneIkkeHenteTiltakshistorikk
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.jobber
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltaker
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration

class OppdatertTiltaksdeltakelseJobbAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `planlagt jobb behandler markører med den nye jobben både lokalt og i nais`() {
        listOf(false, true).forEach { isNais ->
            withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
                val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
                val (sak) = iverksettSøknadsbehandling(
                    tac = tac,
                    tiltaksdeltakelse = deltakelse,
                    innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
                )
                tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseTilOgMed = 5.juni(2025)))
                val repo = tac.tiltakContext.tiltaksdeltakerRepo
                repo.registrerUbehandletEndring(deltakelse.internDeltakelseId, sak.id, nå(tac.clock).minusMinutes(20))
                val task = jobber(isNais, tac, tac.clock).single { it.navn == "saksbehandling-jobb-endret-tiltaksdeltaker" }

                task.utfør(CorrelationId.generate()) shouldBe TaskResultat.Ferdig

                repo.hentTiltaksdeltaker(deltakelse.eksternDeltakelseId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
                val behandlinger = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger
                behandlinger.size shouldBe 2
                behandlinger.last().shouldBeInstanceOf<Revurdering>().automatiskOpprettetGrunn.shouldNotBeNull()
                tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>().opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `tom kø gjør ingen registeroppslag`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val klient = RegistrerendeKlient(tac.tiltakContext.tiltaksdeltakelseKlient)
            val jobb = tac.jobb(klient, tac.clock)

            jobb.håndterUbehandledeEndringer()

            klient.oppslag.shouldBeEmpty()
            tac.tiltakContext.tiltaksdeltakerRepo.hentMedUbehandledeEndringer(nå(tac.clock)).shouldBeEmpty()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `køen velger bare markører eldre enn femten minutter og behandler eldste først`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val utenMarkør = opprettDeltaker(tac)
            val fersk = opprettDeltaker(tac)
            val påGrensen = opprettDeltaker(tac)
            val gammel = opprettDeltaker(tac)
            val eldst = opprettDeltaker(tac)
            val clock = fixedClockAt(nå(tac.clock).withNano(0))
            val tidspunkt = nå(clock)
            val repo = tac.tiltakContext.tiltaksdeltakerRepo
            repo.registrerUbehandletEndring(fersk.id, fersk.sakId, tidspunkt)
            repo.registrerUbehandletEndring(påGrensen.id, påGrensen.sakId, tidspunkt.minusMinutes(15))
            repo.registrerUbehandletEndring(gammel.id, gammel.sakId, tidspunkt.minusMinutes(20))
            repo.registrerUbehandletEndring(eldst.id, eldst.sakId, tidspunkt.minusMinutes(30))

            val kandidater = repo.hentMedUbehandledeEndringer(tidspunkt.minusMinutes(15))
            kandidater.map { it.id } shouldBe listOf(eldst.id, gammel.id)
            kandidater.map { it.sakId } shouldBe listOf(eldst.sakId, gammel.sakId)
            kandidater.map { it.sisteUbehandletEndringTidspunkt } shouldBe
                listOf(tidspunkt.minusMinutes(30), tidspunkt.minusMinutes(20))
            val klient = RegistrerendeKlient(tac.tiltakContext.tiltaksdeltakelseKlient)
            tac.jobb(klient, clock).håndterUbehandledeEndringer()

            klient.oppslag.map { it.second } shouldBe listOf(eldst.eksternId, gammel.eksternId)
            repo.hentMedUbehandledeEndringer(tidspunkt.minusMinutes(15)).shouldBeEmpty()
            repo.hentTiltaksdeltaker(eldst.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
            repo.hentTiltaksdeltaker(gammel.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
            repo.hentTiltaksdeltaker(utenMarkør.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
            repo.hentTiltaksdeltaker(utenMarkør.eksternId).shouldNotBeNull().sakId shouldBe utenMarkør.sakId
            repo.hentTiltaksdeltaker(fersk.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe tidspunkt
            repo.hentTiltaksdeltaker(påGrensen.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe tidspunkt.minusMinutes(15)

            tac.jobb(klient, Clock.offset(clock, Duration.ofSeconds(1))).håndterUbehandledeEndringer()

            klient.oppslag.map { it.second } shouldBe listOf(eldst.eksternId, gammel.eksternId, påGrensen.eksternId)
            repo.hentTiltaksdeltaker(påGrensen.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `flere markører slås sammen og ventetiden regnes fra den siste`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val deltaker = opprettDeltaker(tac)
            val clock = fixedClockAt(nå(tac.clock).withNano(0))
            val sisteMarkør = nå(clock).minusMinutes(5)
            val repo = tac.tiltakContext.tiltaksdeltakerRepo
            repo.registrerUbehandletEndring(deltaker.id, deltaker.sakId, nå(clock).minusMinutes(30))
            repo.registrerUbehandletEndring(deltaker.id, deltaker.sakId, nå(clock).minusMinutes(20))
            repo.registrerUbehandletEndring(deltaker.id, deltaker.sakId, sisteMarkør)
            val klient = RegistrerendeKlient(tac.tiltakContext.tiltaksdeltakelseKlient)

            tac.jobb(klient, clock).håndterUbehandledeEndringer()
            tac.jobb(klient, Clock.offset(clock, Duration.ofMinutes(10))).håndterUbehandledeEndringer()

            klient.oppslag.shouldBeEmpty()
            repo.hentTiltaksdeltaker(deltaker.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe sisteMarkør

            val etterVentetid = tac.jobb(klient, Clock.offset(clock, Duration.ofMinutes(10).plusSeconds(1)))
            etterVentetid.håndterUbehandledeEndringer()
            etterVentetid.håndterUbehandledeEndringer()

            klient.oppslag.map { it.second } shouldBe listOf(deltaker.eksternId)
            repo.hentTiltaksdeltaker(deltaker.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `feil for første deltaker beholder markøren uten å stoppe neste deltaker`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val førsteDeltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val andreDeltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (førsteSak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = førsteDeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(førsteDeltakelse.periode!!, førsteDeltakelse),
            )
            val (andreSak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = andreDeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(andreDeltakelse.periode!!, andreDeltakelse),
            )
            tac.oppdaterTiltaksdeltakelse(andreSak.fnr, andreDeltakelse.copy(deltakelseTilOgMed = 5.juni(2025)))
            val clock = fixedClockAt(nå(tac.clock).withNano(0))
            val førsteMarkør = nå(clock).minusMinutes(30)
            val repo = tac.tiltakContext.tiltaksdeltakerRepo
            repo.registrerUbehandletEndring(førsteDeltakelse.internDeltakelseId, førsteSak.id, førsteMarkør)
            repo.registrerUbehandletEndring(andreDeltakelse.internDeltakelseId, andreSak.id, nå(clock).minusMinutes(20))
            val klient = RegistrerendeKlient(tac.tiltakContext.tiltaksdeltakelseKlient) { eksternId ->
                if (eksternId == førsteDeltakelse.eksternDeltakelseId) error("Registeret er utilgjengelig")
            }

            tac.jobb(klient, clock).håndterUbehandledeEndringer()

            klient.oppslag shouldBe listOf(
                førsteSak.fnr to førsteDeltakelse.eksternDeltakelseId,
                andreSak.fnr to andreDeltakelse.eksternDeltakelseId,
            )
            repo.hentMedUbehandledeEndringer(nå(clock).minusMinutes(15)).map { it.id } shouldBe listOf(førsteDeltakelse.internDeltakelseId)
            repo.hentTiltaksdeltaker(førsteDeltakelse.eksternDeltakelseId).shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe førsteMarkør
            repo.hentTiltaksdeltaker(andreDeltakelse.eksternDeltakelseId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
            tac.sakContext.sakRepo.hentForSakId(førsteSak.id)!!.rammebehandlinger.map { it.id } shouldBe førsteSak.rammebehandlinger.map { it.id }
            val andreBehandlinger = tac.sakContext.sakRepo.hentForSakId(andreSak.id)!!.rammebehandlinger
            andreBehandlinger.size shouldBe 2
            andreBehandlinger.last().shouldBeInstanceOf<Revurdering>()
        }
    }

    private suspend fun ApplicationTestBuilder.opprettDeltaker(tac: TestApplicationContextMedPostgres): Tiltaksdeltaker {
        val deltakelse = tac.tiltaksdeltakelse()
        opprettSakOgSøknad(tac = tac, fnr = ObjectMother.gyldigFnr(), tiltaksdeltakelse = deltakelse)
        return tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(deltakelse.eksternDeltakelseId).shouldNotBeNull()
    }

    private fun TestApplicationContextMedPostgres.jobb(
        klient: TiltaksdeltakelseKlient,
        clock: Clock,
    ) = OppdatertTiltaksdeltakelseJobb(
        tiltaksdeltakerRepo = tiltakContext.tiltaksdeltakerRepo,
        sakRepo = sakContext.sakRepo,
        rammebehandlingRepo = behandlingContext.rammebehandlingRepo,
        tiltaksdeltakelseKlient = klient,
        startRevurderingService = behandlingContext.startRevurderingService,
        clock = clock,
    )

    private class RegistrerendeKlient(
        private val delegate: TiltaksdeltakelseKlient,
        private val førOppslag: (String) -> Unit = {},
    ) : TiltaksdeltakelseKlient by delegate {
        val oppslag = mutableListOf<Pair<Fnr, String>>()

        override suspend fun hentTiltaksdeltakelse(
            fnr: Fnr,
            eksternDeltakerId: String,
            correlationId: CorrelationId,
        ): Either<KunneIkkeHenteTiltakshistorikk, Tiltaksdeltakelse?> {
            oppslag.add(fnr to eksternDeltakerId)
            førOppslag(eksternDeltakerId)
            return delegate.hentTiltaksdeltakelse(fnr, eksternDeltakerId, correlationId)
        }
    }
}
