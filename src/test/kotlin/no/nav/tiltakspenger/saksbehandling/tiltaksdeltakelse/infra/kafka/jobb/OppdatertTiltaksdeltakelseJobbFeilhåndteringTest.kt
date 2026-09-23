package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arenastatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltakstype
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.KunneIkkeHenteTiltakshistorikk
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.testdeltakelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.httpKlientUventetStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltaker
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.util.UUID

class OppdatertTiltaksdeltakelseJobbFeilhåndteringTest {

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `Gosys-feil beholder markøren til oppgaven er opprettet ved nytt forsøk`(kastException: Boolean) {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, deltakelse) = opprettInnvilgetSak(tac)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Venteliste))
            val deltaker = tac.registrerEndring(sak, deltakelse)
            val delegate = tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>()
            var skalFeile = true
            var antallForsøk = 0
            val klient = object : OppgaveKlient by delegate {
                override suspend fun opprettOppgaveUtenDuplikatkontroll(
                    fnr: Fnr,
                    oppgavebehov: Oppgavebehov,
                    tilleggstekst: String?,
                ): Either<HttpKlientError, OppgaveId> {
                    antallForsøk++
                    if (skalFeile) {
                        if (kastException) error("Gosys er utilgjengelig")
                        return httpKlientUventetStatus().left()
                    }
                    return delegate.opprettOppgaveUtenDuplikatkontroll(fnr, oppgavebehov, tilleggstekst)
                }
            }
            val jobb = tac.jobb(oppgaveKlient = klient)

            jobb.behandleDeltaker(deltaker)

            antallForsøk shouldBe 1
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt shouldBe deltaker.sisteUbehandletEndringTidspunkt
            delegate.opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe sak.rammebehandlinger.map { it.id }

            skalFeile = false
            jobb.behandleDeltaker(tac.hentDeltaker(deltakelse))
            jobb.behandleDeltaker(tac.hentDeltaker(deltakelse))

            antallForsøk shouldBe 2
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            delegate.opprettedeOppgaverUtenDuplikatkontroll shouldBe listOf(sak.fnr to Oppgavebehov.ENDRET_TILTAKDELTAKER)
            delegate.opprettedeOppgavetekster shouldBe listOf("Endret status.")
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe sak.rammebehandlinger.map { it.id }
        }
    }

    @Test
    fun `ny markør under oppgaveopprettelsen beholdes til neste kjøring`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, deltakelse) = opprettInnvilgetSak(tac)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Venteliste))
            val deltaker = tac.registrerEndring(sak, deltakelse)
            val nyMarkør = nå(tac.clock).withNano(0)
            val delegate = tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>()
            val klient = object : OppgaveKlient by delegate {
                override suspend fun opprettOppgaveUtenDuplikatkontroll(
                    fnr: Fnr,
                    oppgavebehov: Oppgavebehov,
                    tilleggstekst: String?,
                ): Either<HttpKlientError, OppgaveId> {
                    val oppgave = delegate.opprettOppgaveUtenDuplikatkontroll(fnr, oppgavebehov, tilleggstekst)
                    tac.tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(deltaker.id, sak.id, nyMarkør)
                    tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseTilOgMed = 5.juni(2025)))
                    return oppgave
                }
            }
            val jobb = tac.jobb(oppgaveKlient = klient)

            jobb.behandleDeltaker(deltaker)

            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt shouldBe nyMarkør
            delegate.opprettedeOppgaverUtenDuplikatkontroll shouldBe listOf(sak.fnr to Oppgavebehov.ENDRET_TILTAKDELTAKER)

            jobb.behandleDeltaker(tac.hentDeltaker(deltakelse))

            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            delegate.opprettedeOppgaverUtenDuplikatkontroll shouldBe listOf(sak.fnr to Oppgavebehov.ENDRET_TILTAKDELTAKER)
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `registerfeil beholder markøren og vellykket retry oppretter bare én revurdering`(kastException: Boolean) {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, deltakelse) = opprettInnvilgetSak(tac)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseTilOgMed = 5.juni(2025)))
            val deltaker = tac.registrerEndring(sak, deltakelse)
            var skalFeile = true
            var antallOppslag = 0
            val delegate = tac.tiltakContext.tiltaksdeltakelseKlient
            val klient = StyrtKlient(delegate) { fnr, eksternId, correlationId ->
                antallOppslag++
                if (skalFeile) {
                    if (kastException) error("Registeret er utilgjengelig")
                    KunneIkkeHenteTiltakshistorikk.KallFeilet(httpKlientUventetStatus()).left()
                } else {
                    delegate.hentTiltaksdeltakelse(fnr, eksternId, correlationId)
                }
            }
            val jobb = tac.jobb(klient = klient)

            jobb.behandleDeltaker(deltaker)

            antallOppslag shouldBe 1
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt shouldBe deltaker.sisteUbehandletEndringTidspunkt
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe sak.rammebehandlinger.map { it.id }

            skalFeile = false
            jobb.behandleDeltaker(tac.hentDeltaker(deltakelse))

            antallOppslag shouldBe 2
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            val etterRetry = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            etterRetry.rammebehandlinger.size shouldBe 2
            etterRetry.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
                .automatiskOpprettetGrunn.shouldNotBeNull().endringer shouldBe
                listOf(TiltaksdeltakerEndring.Forlengelse(5.juni(2025)))

            jobb.behandleDeltaker(deltaker)
            jobb.behandleDeltaker(tac.hentDeltaker(deltakelse))

            antallOppslag shouldBe 3
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe etterRetry.rammebehandlinger.map { it.id }
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>().opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
        }
    }

    @Test
    fun `ny markør under registeroppslaget overlever gammel kvittering og behandles neste gang`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, deltakelse) = opprettInnvilgetSak(tac)
            val deltaker = tac.registrerEndring(sak, deltakelse)
            val repo = tac.tiltakContext.tiltaksdeltakerRepo
            val nyMarkør = nå(tac.clock).withNano(0)
            var antallOppslag = 0
            val delegate = tac.tiltakContext.tiltaksdeltakelseKlient
            val klient = StyrtKlient(delegate) { fnr, eksternId, correlationId ->
                antallOppslag++
                val svar = delegate.hentTiltaksdeltakelse(fnr, eksternId, correlationId)
                if (antallOppslag == 1) {
                    // Den nye hendelsen kommer etter registerets snapshot, men før jobbens kvittering.
                    repo.registrerUbehandletEndring(deltaker.id, sak.id, nyMarkør)
                    tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseTilOgMed = 5.juni(2025)))
                }
                svar
            }
            val jobb = tac.jobb(klient = klient)

            jobb.behandleDeltaker(deltaker)

            antallOppslag shouldBe 1
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt shouldBe nyMarkør
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe sak.rammebehandlinger.map { it.id }

            jobb.behandleDeltaker(tac.hentDeltaker(deltakelse))

            antallOppslag shouldBe 2
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            val etterNyMarkør = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            etterNyMarkør.rammebehandlinger.size shouldBe 2
            etterNyMarkør.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
                .automatiskOpprettetGrunn.shouldNotBeNull().endringer shouldBe
                listOf(TiltaksdeltakerEndring.Forlengelse(5.juni(2025)))
        }
    }

    @Test
    fun `retry etter feil i kvitteringen beholder lagret revurdering uten duplikat og rydder markøren`() {
        withTestApplicationContextAndPostgres { tac ->
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

            jobb.behandleDeltaker(deltaker)

            antallKvitteringer shouldBe 1
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt shouldBe deltaker.sisteUbehandletEndringTidspunkt
            val etterFeiletKvittering = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            etterFeiletKvittering.rammebehandlinger.size shouldBe 2
            etterFeiletKvittering.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
                .automatiskOpprettetGrunn.shouldNotBeNull().endringer shouldBe
                listOf(TiltaksdeltakerEndring.Forlengelse(5.juni(2025)))

            jobb.behandleDeltaker(tac.hentDeltaker(deltakelse))

            antallKvitteringer shouldBe 2
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe etterFeiletKvittering.rammebehandlinger.map { it.id }
            tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>().opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `ukjent status eller tiltakstype fra registeret kvitteres uten revurdering`(ukjentStatus: Boolean) {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, deltakelse) = opprettInnvilgetSak(tac)
            val deltaker = tac.registrerEndring(sak, deltakelse)
            val uleseligDeltakelse = if (ukjentStatus) {
                testdeltakelse(
                    id = deltakelse.eksternDeltakelseId,
                    kildestatus = Arenastatus.Ukjent("NY_STATUS"),
                    fraOgMed = deltakelse.deltakelseFraOgMed,
                    tilOgMed = 5.juni(2025),
                )
            } else {
                testdeltakelse(
                    id = deltakelse.eksternDeltakelseId,
                    tiltakstype = Tiltakstype.Ukjent(tiltakskodeFraKilden = "NY_TILTAKSTYPE"),
                    fraOgMed = deltakelse.deltakelseFraOgMed,
                    tilOgMed = 5.juni(2025),
                )
            }
            val oppslag = mutableListOf<Pair<Fnr, String>>()
            val klient = StyrtKlient(tac.tiltakContext.tiltaksdeltakelseKlient) { fnr, eksternId, _ ->
                oppslag.add(fnr to eksternId)
                uleseligDeltakelse.right()
            }

            tac.jobb(klient = klient).behandleDeltaker(deltaker)

            oppslag shouldBe listOf(sak.fnr to deltakelse.eksternDeltakelseId)
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe sak.rammebehandlinger.map { it.id }
            tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>().opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
        }
    }

    @Test
    fun `deltakelse flyttet fra Arena til Komet slås opp med ny ekstern id`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025)).let {
                it.copy(eksternDeltakelseId = "TA${it.eksternDeltakelseId.substringAfterLast('_')}", kilde = Tiltakskilde.Arena)
            }
            val (sak) = opprettInnvilgetSak(tac, deltakelse)
            val kometId = UUID.randomUUID().toString()

            tac.tiltaksdeltakerArenaConsumer.consume(
                deltakelse.eksternDeltakelseId.removePrefix("TA"),
                """
                {
                  "op_type": "U",
                  "after": {
                    "ANTALL_DAGER_PR_UKE": 5.0,
                    "PROSENT_DELTID": 100.0,
                    "DELTAKERSTATUSKODE": "GJENN",
                    "DATO_FRA": "2025-01-05 00:00:00",
                    "DATO_TIL": "2025-05-05 00:00:00",
                    "EKSTERN_ID": "$kometId"
                  }
                }
                """.trimIndent(),
            )
            val repo = tac.tiltakContext.tiltaksdeltakerRepo
            repo.hentTiltaksdeltaker(deltakelse.eksternDeltakelseId).shouldBeNull()
            val flyttetDeltaker = repo.hentTiltaksdeltaker(kometId).shouldNotBeNull()
            flyttetDeltaker.id shouldBe deltakelse.internDeltakelseId
            flyttetDeltaker.utdatertEksternId shouldBe deltakelse.eksternDeltakelseId
            flyttetDeltaker.sisteUbehandletEndringTidspunkt.shouldNotBeNull()
            tac.oppdaterTiltaksdeltakelse(sak.fnr, null)
            tac.oppdaterTiltaksdeltakelse(
                sak.fnr,
                deltakelse.copy(
                    eksternDeltakelseId = kometId,
                    kilde = Tiltakskilde.Komet,
                    deltakelseTilOgMed = 5.juni(2025),
                ),
            )
            val delegate = tac.tiltakContext.tiltaksdeltakelseKlient
            val oppslag = mutableListOf<Pair<Fnr, String>>()
            val klient = StyrtKlient(delegate) { fnr, eksternId, correlationId ->
                oppslag.add(fnr to eksternId)
                delegate.hentTiltaksdeltakelse(fnr, eksternId, correlationId)
            }

            tac.jobb(klient = klient).behandleDeltaker(flyttetDeltaker)

            oppslag shouldBe listOf(sak.fnr to kometId)
            repo.hentTiltaksdeltaker(kometId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            oppdatertSak.rammebehandlinger.size shouldBe 2
            oppdatertSak.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
                .automatiskOpprettetGrunn.shouldNotBeNull().endringer shouldBe
                listOf(TiltaksdeltakerEndring.Forlengelse(5.juni(2025)))
        }
    }

    private suspend fun ApplicationTestBuilder.opprettInnvilgetSak(
        tac: TestApplicationContextMedPostgres,
        deltakelse: TiltaksdeltakelseIntern = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025)),
    ): Pair<Sak, TiltaksdeltakelseIntern> {
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

    private fun TestApplicationContextMedPostgres.jobb(
        klient: TiltaksdeltakelseKlient = tiltakContext.tiltaksdeltakelseKlient,
        repo: TiltaksdeltakerRepo = tiltakContext.tiltaksdeltakerRepo,
        oppgaveKlient: OppgaveKlient = this.oppgaveKlient,
    ) = OppdatertTiltaksdeltakelseJobb(
        tiltaksdeltakerRepo = repo,
        sakRepo = sakContext.sakRepo,
        rammebehandlingRepo = behandlingContext.rammebehandlingRepo,
        tiltaksdeltakelseKlient = klient,
        startRevurderingService = behandlingContext.startRevurderingService,
        oppgaveKlient = oppgaveKlient,
        eksternOppgaveRepo = eksternOppgaveRepo,
        sessionFactory = sessionFactory,
        clock = clock,
    )

    private class StyrtKlient(
        delegate: TiltaksdeltakelseKlient,
        private val hent: suspend (Fnr, String, CorrelationId) -> Either<KunneIkkeHenteTiltakshistorikk, Tiltaksdeltakelse?>,
    ) : TiltaksdeltakelseKlient by delegate {
        override suspend fun hentTiltaksdeltakelse(
            fnr: Fnr,
            eksternDeltakerId: String,
            correlationId: CorrelationId,
        ): Either<KunneIkkeHenteTiltakshistorikk, Tiltaksdeltakelse?> = hent(fnr, eksternDeltakerId, correlationId)
    }
}
