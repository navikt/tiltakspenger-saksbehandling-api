package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import arrow.core.Either
import arrow.core.left
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
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
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.KunneIkkeHenteTiltakshistorikk
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.jobber
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgave
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgaveRepo
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltaker
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

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

    @Test
    @IsolatedDatabaseTest
    fun `ny markør under oppgaveopprettelsen beholdes til neste kjøring`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, deltakelse) = opprettInnvilgetSak(tac)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Venteliste))
            tac.registrerEndring(sak, deltakelse)
            val nyMarkør = nå(tac.clock).withNano(0)
            val delegate = tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>()
            val klient = object : OppgaveKlient by delegate {
                override suspend fun opprettOppgaveUtenDuplikatkontroll(
                    fnr: Fnr,
                    oppgavebehov: Oppgavebehov,
                    tilleggstekst: String?,
                ): Either<HttpKlientError, OppgaveId> {
                    val oppgave = delegate.opprettOppgaveUtenDuplikatkontroll(fnr, oppgavebehov, tilleggstekst)
                    tac.tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(deltakelse.internDeltakelseId, sak.id, nyMarkør)
                    tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseTilOgMed = 5.juni(2025)))
                    return oppgave
                }
            }

            tac.jobb(oppgaveKlient = klient).håndterUbehandledeEndringer()

            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt shouldBe nyMarkør
            delegate.opprettedeOppgaverUtenDuplikatkontroll shouldBe listOf(sak.fnr to Oppgavebehov.ENDRET_TILTAKDELTAKER)

            tac.jobb(oppgaveKlient = klient, clock = etterVentetiden(tac.clock)).håndterUbehandledeEndringer()

            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            delegate.opprettedeOppgaverUtenDuplikatkontroll shouldBe listOf(sak.fnr to Oppgavebehov.ENDRET_TILTAKDELTAKER)
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `ny markør under registeroppslaget overlever gammel kvittering og behandles neste gang`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, deltakelse) = opprettInnvilgetSak(tac)
            tac.registrerEndring(sak, deltakelse)
            val repo = tac.tiltakContext.tiltaksdeltakerRepo
            val nyMarkør = nå(tac.clock).withNano(0)
            val klient = RegistrerendeKlient(
                delegate = tac.tiltakContext.tiltaksdeltakelseKlient,
                etterOppslag = { antallOppslag ->
                    if (antallOppslag == 1) {
                        // Den nye hendelsen kommer etter registerets snapshot, men før jobbens kvittering.
                        repo.registrerUbehandletEndring(deltakelse.internDeltakelseId, sak.id, nyMarkør)
                        tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseTilOgMed = 5.juni(2025)))
                    }
                },
            )

            tac.jobb(klient).håndterUbehandledeEndringer()

            klient.oppslag.size shouldBe 1
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt shouldBe nyMarkør
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe sak.rammebehandlinger.map { it.id }

            tac.jobb(klient, etterVentetiden(tac.clock)).håndterUbehandledeEndringer()

            klient.oppslag.size shouldBe 2
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            val etterNyMarkør = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            etterNyMarkør.rammebehandlinger.size shouldBe 2
            etterNyMarkør.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
                .automatiskOpprettetGrunn.shouldNotBeNull().endring shouldBe
                TiltaksdeltakerEndring.Forlengelse(5.juni(2025))
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `retry etter feil i kvitteringen beholder lagret revurdering uten duplikat og rydder markøren`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, deltakelse) = opprettInnvilgetSak(tac)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseTilOgMed = 5.juni(2025)))
            val deltaker = tac.registrerEndring(sak, deltakelse)
            val delegate = tac.tiltakContext.tiltaksdeltakerRepo
            var antallKvitteringer = 0
            val repo = object : TiltaksdeltakerRepo by delegate {
                override fun markerEndringSomBehandlet(
                    id: TiltaksdeltakerId,
                    forventetSisteUbehandletEndring: LocalDateTime,
                    sessionContext: SessionContext?,
                ) {
                    antallKvitteringer++
                    if (antallKvitteringer == 1) error("Kvitteringen kunne ikke lagres")
                    delegate.markerEndringSomBehandlet(id, forventetSisteUbehandletEndring, sessionContext)
                }
            }
            val jobb = tac.jobb(repo = repo)

            jobb.håndterUbehandledeEndringer()

            antallKvitteringer shouldBe 1
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt shouldBe deltaker.sisteUbehandletEndringTidspunkt
            val etterFeiletKvittering = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            etterFeiletKvittering.rammebehandlinger.size shouldBe 2
            etterFeiletKvittering.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
                .automatiskOpprettetGrunn.shouldNotBeNull().endring shouldBe
                TiltaksdeltakerEndring.Forlengelse(5.juni(2025))

            jobb.håndterUbehandledeEndringer()

            antallKvitteringer shouldBe 2
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe etterFeiletKvittering.rammebehandlinger.map { it.id }
            tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>().opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
        }
    }

    /** Feil som returneres som Left, og ikke kastes, skal også beholde markøren. */
    @Test
    @IsolatedDatabaseTest
    fun `registerfeil og Gosys-feil beholder markøren uten å stoppe neste deltaker`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (registerfeilSak, registerfeilDeltakelse) = opprettInnvilgetSak(tac)
            val (gosysfeilSak, gosysfeilDeltakelse) = opprettInnvilgetSak(tac)
            val (okSak, okDeltakelse) = opprettInnvilgetSak(tac)
            tac.oppdaterTiltaksdeltakelse(gosysfeilSak.fnr, gosysfeilDeltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Venteliste))
            tac.oppdaterTiltaksdeltakelse(okSak.fnr, okDeltakelse.copy(deltakelseTilOgMed = 5.juni(2025)))
            val registerfeil = tac.registrerEndring(registerfeilSak, registerfeilDeltakelse)
            val gosysfeil = tac.registrerEndring(gosysfeilSak, gosysfeilDeltakelse)
            tac.registrerEndring(okSak, okDeltakelse)
            val delegate = tac.tiltakContext.tiltaksdeltakelseKlient
            val klient = object : TiltaksdeltakelseKlient by delegate {
                override suspend fun hentTiltaksdeltakelse(
                    fnr: Fnr,
                    eksternDeltakerId: String,
                    correlationId: CorrelationId,
                ): Either<KunneIkkeHenteTiltakshistorikk, Tiltaksdeltakelse?> =
                    if (eksternDeltakerId == registerfeilDeltakelse.eksternDeltakelseId) {
                        KunneIkkeHenteTiltakshistorikk.KallFeilet(ObjectMother.httpKlientUventetStatus()).left()
                    } else {
                        delegate.hentTiltaksdeltakelse(fnr, eksternDeltakerId, correlationId)
                    }
            }
            val oppgaver = tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>()
            oppgaver.opprettOppgaveUtenDuplikatkontrollResponse = ObjectMother.httpKlientUventetStatus().left()

            tac.jobb(klient = klient).håndterUbehandledeEndringer()

            tac.hentDeltaker(registerfeilDeltakelse).sisteUbehandletEndringTidspunkt shouldBe registerfeil.sisteUbehandletEndringTidspunkt
            tac.hentDeltaker(gosysfeilDeltakelse).sisteUbehandletEndringTidspunkt shouldBe gosysfeil.sisteUbehandletEndringTidspunkt
            tac.eksternOppgaveRepo.hentForSakId(gosysfeilSak.id).shouldBeEmpty()
            tac.hentDeltaker(okDeltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            tac.sakContext.sakRepo.hentForSakId(okSak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
        }
    }

    /**
     * Gosys-oppgaven, referansen til den og kvitteringen av markøren lagres hver for seg.
     * Feiler lagringen av referansen, står markøren igjen, og neste kjøring oppretter en ny Gosys-oppgave.
     */
    @Test
    @IsolatedDatabaseTest
    fun `feil ved lagring av oppgavereferansen beholder markøren og gir ny Gosys-oppgave ved neste kjøring`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, deltakelse) = opprettInnvilgetSak(tac)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Venteliste))
            val deltaker = tac.registrerEndring(sak, deltakelse)
            val delegate = tac.eksternOppgaveRepo
            var antallLagringer = 0
            val repo = object : EksternOppgaveRepo by delegate {
                override fun lagre(eksternOppgave: EksternOppgave, sessionContext: SessionContext?) {
                    antallLagringer++
                    if (antallLagringer == 1) error("Oppgavereferansen kunne ikke lagres")
                    delegate.lagre(eksternOppgave, sessionContext)
                }
            }
            val jobb = tac.jobb(eksternOppgaveRepo = repo)
            val oppgaver = tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>()

            jobb.håndterUbehandledeEndringer()

            oppgaver.opprettedeOppgaveIder shouldHaveSize 1
            tac.eksternOppgaveRepo.hentForSakId(sak.id).shouldBeEmpty()
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt shouldBe deltaker.sisteUbehandletEndringTidspunkt

            jobb.håndterUbehandledeEndringer()

            oppgaver.opprettedeOppgaveIder shouldHaveSize 2
            tac.eksternOppgaveRepo.hentForSakId(sak.id).map { it.oppgaveId } shouldBe listOf(oppgaver.opprettedeOppgaveIder.last())
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
        }
    }

    private suspend fun ApplicationTestBuilder.opprettDeltaker(tac: TestApplicationContextMedPostgres): Tiltaksdeltaker {
        val deltakelse = tac.tiltaksdeltakelse()
        opprettSakOgSøknad(tac = tac, fnr = ObjectMother.gyldigFnr(), tiltaksdeltakelse = deltakelse)
        return tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(deltakelse.eksternDeltakelseId).shouldNotBeNull()
    }

    private suspend fun ApplicationTestBuilder.opprettInnvilgetSak(
        tac: TestApplicationContextMedPostgres,
    ): Pair<Sak, TiltaksdeltakelseIntern> {
        val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
        val (sak) = iverksettSøknadsbehandling(
            tac = tac,
            tiltaksdeltakelse = deltakelse,
            innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
        )
        return sak to deltakelse
    }

    private fun TestApplicationContextMedPostgres.registrerEndring(
        sak: Sak,
        deltakelse: TiltaksdeltakelseIntern,
    ): Tiltaksdeltaker {
        tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(
            id = deltakelse.internDeltakelseId,
            sakId = sak.id,
            tidspunkt = nå(clock).minusMinutes(20).withNano(0),
        )
        return hentDeltaker(deltakelse)
    }

    private fun TestApplicationContextMedPostgres.hentDeltaker(deltakelse: TiltaksdeltakelseIntern): Tiltaksdeltaker =
        tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(deltakelse.eksternDeltakelseId).shouldNotBeNull()

    /** En klokke langt nok fram til at markører satt nå er eldre enn jobbens forsinkelse. */
    private fun etterVentetiden(clock: Clock): Clock =
        Clock.offset(clock, Duration.ofMinutes(OppdatertTiltaksdeltakelseJobb.MINUTTER_FORSINKELSE + 1))

    private fun TestApplicationContextMedPostgres.jobb(
        klient: TiltaksdeltakelseKlient = tiltakContext.tiltaksdeltakelseKlient,
        clock: Clock = this.clock,
        repo: TiltaksdeltakerRepo = tiltakContext.tiltaksdeltakerRepo,
        oppgaveKlient: OppgaveKlient = this.oppgaveKlient,
        eksternOppgaveRepo: EksternOppgaveRepo = this.eksternOppgaveRepo,
    ) = OppdatertTiltaksdeltakelseJobb(
        tiltaksdeltakerRepo = repo,
        sakRepo = sakContext.sakRepo,
        rammebehandlingRepo = behandlingContext.rammebehandlingRepo,
        tiltaksdeltakelseKlient = klient,
        startRevurderingService = behandlingContext.startRevurderingService,
        oppgaveKlient = oppgaveKlient,
        eksternOppgaveRepo = eksternOppgaveRepo,
        clock = clock,
    )

    private class RegistrerendeKlient(
        private val delegate: TiltaksdeltakelseKlient,
        private val etterOppslag: (antallOppslag: Int) -> Unit = {},
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
                .also { etterOppslag(oppslag.size) }
        }
    }
}
