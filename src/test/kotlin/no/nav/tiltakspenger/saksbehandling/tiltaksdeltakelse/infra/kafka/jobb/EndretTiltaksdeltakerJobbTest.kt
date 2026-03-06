package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppgaveId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.getTiltaksdeltakerKafkaDb
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class EndretTiltaksdeltakerJobbTest {
    private val oppgaveId = oppgaveId()

    @Test
    fun `ingen opprettet behandling - sletter fra db`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()
            val deltakelsesperiode = 5.januar(2025) til 5.mai(2025)

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            // Opprett sak og søknad uten å starte behandling
            val (sak) = opprettSakOgSøknad(
                tac = tac,
                fnr = fnr,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                sakId = sak.id,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )

            tac.tiltaksdeltakerKafkaRepository.lagre(
                tiltaksdeltakerKafkaDb,
                "melding",
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.opprettOppgaveEllerRevurderingForEndredeDeltakere()

            tac.tiltaksdeltakerKafkaRepository.hent(tiltaksdeltakerKafkaDb.id) shouldBe null
        }
    }

    @Test
    fun `ingen behandling for endret deltaker - sletter fra db`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val fnr = Fnr.random()
            val deltakelsesperiode = 5.januar(2025) til 5.mai(2025)

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
            )

            val (sak) = opprettSøknadsbehandlingUnderBehandling(
                tac = tac,
                fnr = fnr,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            // Opprett tiltaksdeltaker for en annen deltakerId enn den i behandlingen
            val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                sakId = sak.id,
            )

            tac.tiltaksdeltakerKafkaRepository.lagre(
                tiltaksdeltakerKafkaDb,
                "melding",
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.opprettOppgaveEllerRevurderingForEndredeDeltakere()

            tac.tiltaksdeltakerKafkaRepository.hent(tiltaksdeltakerKafkaDb.id) shouldBe null
        }
    }

    @Test
    fun `åpen behandling for endret deltaker - oppretter oppgave, ikke revurdering`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()
            val deltakelseFom = LocalDate.now(tac.clock).minusDays(2)
            val deltakelsesTom = LocalDate.now(tac.clock).plusMonths(3)
            val deltakelsesperiode = deltakelseFom til deltakelsesTom

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            val (sak) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac = tac,
                fnr = fnr,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelsesperiode, tiltaksdeltakelse),
            )

            val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                sakId = sak.id,
                deltakerstatus = TiltakDeltakerstatus.IkkeAktuell,
                fom = null,
                tom = null,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerKafkaRepository.lagre(
                tiltaksdeltakerKafkaDb,
                "melding",
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.opprettOppgaveEllerRevurderingForEndredeDeltakere()

            val oppdatertTiltaksdeltakerKafkaDb = tac.tiltaksdeltakerKafkaRepository.hent(tiltaksdeltakerKafkaDb.id)
            oppdatertTiltaksdeltakerKafkaDb.shouldNotBeNull()
            oppdatertTiltaksdeltakerKafkaDb.oppgaveId shouldBe oppgaveId
            oppdatertTiltaksdeltakerKafkaDb.behandlingId.shouldBeNull()
        }
    }

    @Test
    fun `åpen automatisk behandling for endret deltaker - oppdaterer venterTil, oppretter ikke oppgave`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()

            val deltakelseFom = LocalDate.now(tac.clock).plusDays(2)
            val deltakelsesTom = LocalDate.now(tac.clock).plusMonths(3)
            val deltakelsesperiode = deltakelseFom til deltakelsesTom

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            val (sak, _, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                tac = tac,
                fnr = fnr,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val kommando = SettRammebehandlingPåVentKommando(
                sakId = behandling.sakId,
                rammebehandlingId = behandling.id,
                begrunnelse = "Tiltaksdeltakelsen har ikke startet ennå",
                saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                venterTil = deltakelseFom.atStartOfDay(),
                frist = null,
            )

            val behandlingPaVent = behandling.settPåVent(kommando = kommando, clock = tac.clock) as Søknadsbehandling
            tac.behandlingContext.rammebehandlingRepo.lagre(behandlingPaVent)

            val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                sakId = sak.id,
                deltakerstatus = TiltakDeltakerstatus.IkkeAktuell,
                fom = null,
                tom = null,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerKafkaRepository.lagre(
                tiltaksdeltakerKafkaDb,
                "melding",
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.opprettOppgaveEllerRevurderingForEndredeDeltakere()

            tac.tiltaksdeltakerKafkaRepository.hent(tiltaksdeltakerKafkaDb.id) shouldBe null
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).venterTil?.toLocalDate() shouldBe 1.mai(2025)
        }
    }

    @Test
    fun `iverksatt behandling, ingen endring - sletter fra db`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()
            val deltakelseFom = 5.januar(2025)
            val deltakelsesTom = 5.mai(2025)
            val deltakelsesperiode = deltakelseFom til deltakelsesTom

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(deltakelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                sakId = sak.id,
                fom = deltakelseFom,
                tom = deltakelsesTom,
                dagerPerUke = 5F,
                deltakelsesprosent = 100F,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerKafkaRepository.lagre(
                tiltaksdeltakerKafkaDb,
                "melding",
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.opprettOppgaveEllerRevurderingForEndredeDeltakere()

            tac.tiltaksdeltakerKafkaRepository.hent(tiltaksdeltakerKafkaDb.id) shouldBe null
        }
    }

    @Test
    fun `iverksatt behandling, forlengelse, deltakelsesmengde - oppretter revurdering`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val deltakelseFom = 5.januar(2025)
            val deltakelsesTom = 5.mai(2025)
            val tiltaksdeltakerId = TiltaksdeltakerId.random()

            val deltakelsesperiode = deltakelseFom til deltakelsesTom

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                fnr = Fnr.random(),
                innvilgelsesperioder = innvilgelsesperioder(deltakelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                sakId = sak.id,
                fom = deltakelseFom,
                tom = deltakelsesTom.plusMonths(1),
                tiltaksdeltakerId = tiltaksdeltakerId,
            )

            tac.tiltaksdeltakerKafkaRepository.lagre(
                tiltaksdeltakerKafkaDb,
                "melding",
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.opprettOppgaveEllerRevurderingForEndredeDeltakere()

            val oppdatertTiltaksdeltakerKafkaDb = tac.tiltaksdeltakerKafkaRepository.hent(tiltaksdeltakerKafkaDb.id)

            val sisteBehandling = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last()

            oppdatertTiltaksdeltakerKafkaDb.shouldNotBeNull()
            oppdatertTiltaksdeltakerKafkaDb.oppgaveId.shouldBeNull()
            oppdatertTiltaksdeltakerKafkaDb.behandlingId shouldBe sisteBehandling.id
        }
    }

    @Test
    fun `iverksatt behandling, avbrutt - oppretter revurdering`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()
            val deltakelseFom = 5.januar(2025)
            val deltakelsesTom = 5.mai(2025)
            val deltakelsesperiode = deltakelseFom til deltakelsesTom

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(deltakelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                sakId = sak.id,
                fom = deltakelseFom,
                tom = deltakelsesTom.minusDays(2),
                deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerKafkaRepository.lagre(
                tiltaksdeltakerKafkaDb,
                "melding",
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.opprettOppgaveEllerRevurderingForEndredeDeltakere()

            val oppdatertTiltaksdeltakerKafkaDb = tac.tiltaksdeltakerKafkaRepository.hent(tiltaksdeltakerKafkaDb.id)
            oppdatertTiltaksdeltakerKafkaDb shouldNotBe null
            oppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe null
        }
    }

    @Nested
    inner class `OpprettOppgaveForEndredeDeltakere - flere vedtak` {

        @Test
        fun `innvilgelse + stans (over hele perioden) lager ikke oppgave eller revurdering`() {
            withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
                val fnr = Fnr.random()
                val førsteTiltaksdeltakerId = TiltaksdeltakerId.random()
                val førsteDeltakelseFom = 5.januar(2025)
                val førsteDeltakelsesTom = 5.mai(2025)
                val førsteDeltakelsesperiode = førsteDeltakelseFom til førsteDeltakelsesTom

                val førsteTiltaksdeltakelse = tiltaksdeltakelse(
                    periode = førsteDeltakelsesperiode,
                    internDeltakelseId = førsteTiltaksdeltakerId,
                )

                val (sak, _, rammevedtak) = iverksettSøknadsbehandling(
                    tac = tac,
                    fnr = fnr,
                    innvilgelsesperioder = innvilgelsesperioder(førsteDeltakelsesperiode, førsteTiltaksdeltakelse),
                    tiltaksdeltakelse = førsteTiltaksdeltakelse,
                )

                iverksettRevurderingStans(
                    tac = tac,
                    sakId = sak.id,
                    stansFraOgMed = rammevedtak.fraOgMed,
                )

                val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    sakId = sak.id,
                    fom = førsteDeltakelseFom,
                    tom = LocalDate.now(tac.clock),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = førsteTiltaksdeltakerId,
                )

                tac.tiltaksdeltakerKafkaRepository.lagre(
                    tiltaksdeltakerKafkaDb,
                    "melding",
                    nå(tac.clock).minusMinutes(20),
                )

                tac.endretTiltaksdeltakerJobb.opprettOppgaveEllerRevurderingForEndredeDeltakere()

                tac.tiltaksdeltakerKafkaRepository.hent(tiltaksdeltakerKafkaDb.id) shouldBe null
            }
        }

        @Test
        fun `innvilgelse + avslag, oppretter stans ved avbrudd`() {
            withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
                val fnr = Fnr.random()
                val førsteTiltaksdeltakerId = TiltaksdeltakerId.random()
                val førsteEksternId = UUID.randomUUID().toString()
                val førsteDeltakelseFom = 5.januar(2025)
                val førsteDeltakelsesTom = 5.mai(2025)
                val førsteDeltakelsesperiode = førsteDeltakelseFom til førsteDeltakelsesTom

                val andreTiltaksdeltakerId = TiltaksdeltakerId.random()
                val andreEksternId = UUID.randomUUID().toString()
                val andreDeltakelseFom = 10.mai(2025)
                val andreDeltakelsesTom = 11.juni(2025)
                val andreDeltakelsesperiode = andreDeltakelseFom til andreDeltakelsesTom

                val førsteTiltaksdeltakelse = tiltaksdeltakelse(
                    periode = førsteDeltakelsesperiode,
                    internDeltakelseId = førsteTiltaksdeltakerId,
                    eksternTiltaksdeltakelseId = førsteEksternId,
                )

                val andreTiltaksdeltakelse = tiltaksdeltakelse(
                    periode = andreDeltakelsesperiode,
                    internDeltakelseId = andreTiltaksdeltakerId,
                    eksternTiltaksdeltakelseId = andreEksternId,
                )

                val (sak) = iverksettSøknadsbehandling(
                    tac = tac,
                    fnr = fnr,
                    innvilgelsesperioder = innvilgelsesperioder(førsteDeltakelsesperiode, førsteTiltaksdeltakelse),
                    tiltaksdeltakelse = førsteTiltaksdeltakelse,
                )

                iverksettSøknadsbehandling(
                    tac = tac,
                    sakId = sak.id,
                    resultat = SøknadsbehandlingsresultatType.AVSLAG,
                    innvilgelsesperioder = innvilgelsesperioder(andreDeltakelsesperiode, andreTiltaksdeltakelse),
                    tiltaksdeltakelse = andreTiltaksdeltakelse,
                )

                val førsteTiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = førsteEksternId,
                    sakId = sak.id,
                    fom = førsteDeltakelseFom,
                    tom = LocalDate.now(tac.clock),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = førsteTiltaksdeltakerId,
                )
                val andreTiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = andreEksternId,
                    sakId = sak.id,
                    fom = andreDeltakelseFom,
                    tom = LocalDate.now(tac.clock),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = andreTiltaksdeltakerId,
                )

                tac.tiltaksdeltakerKafkaRepository.lagre(
                    førsteTiltaksdeltakerKafkaDb,
                    "melding",
                    nå(tac.clock).minusMinutes(20),
                )
                tac.tiltaksdeltakerKafkaRepository.lagre(
                    andreTiltaksdeltakerKafkaDb,
                    "melding",
                    nå(tac.clock).minusMinutes(20),
                )

                tac.endretTiltaksdeltakerJobb.opprettOppgaveEllerRevurderingForEndredeDeltakere()

                val førsteOppdatertTiltaksdeltakerKafkaDb =
                    tac.tiltaksdeltakerKafkaRepository.hent(førsteTiltaksdeltakerKafkaDb.id)
                førsteOppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                førsteOppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe null

                val andreOppdatertTiltaksdeltakerKafkaDb =
                    tac.tiltaksdeltakerKafkaRepository.hent(andreTiltaksdeltakerKafkaDb.id)
                andreOppdatertTiltaksdeltakerKafkaDb shouldBe null
            }
        }

        @Test
        fun `avslag + innvilgelse, oppretter stans ved avbrudd`() {
            withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
                val fnr = Fnr.random()

                val førsteTiltaksdeltakerId = TiltaksdeltakerId.random()
                val førsteEksternId = UUID.randomUUID().toString()
                val førsteDeltakelseFom = 5.januar(2025)
                val førsteDeltakelsesTom = 5.mai(2025)
                val førsteDeltakelsesperiode = førsteDeltakelseFom til førsteDeltakelsesTom

                val andreTiltaksdeltakerId = TiltaksdeltakerId.random()
                val andreEksternId = UUID.randomUUID().toString()
                val andreDeltakelseFom = 10.mai(2025)
                val andreDeltakelsesTom = 11.juni(2025)
                val andreDeltakelsesperiode = andreDeltakelseFom til andreDeltakelsesTom

                val førsteTiltaksdeltakelse = tiltaksdeltakelse(
                    periode = førsteDeltakelsesperiode,
                    internDeltakelseId = førsteTiltaksdeltakerId,
                    eksternTiltaksdeltakelseId = førsteEksternId,
                )

                val andreTiltaksdeltakelse = tiltaksdeltakelse(
                    periode = andreDeltakelsesperiode,
                    internDeltakelseId = andreTiltaksdeltakerId,
                    eksternTiltaksdeltakelseId = andreEksternId,
                )

                val (sak) = iverksettSøknadsbehandling(
                    tac = tac,
                    fnr = fnr,
                    resultat = SøknadsbehandlingsresultatType.AVSLAG,
                    tiltaksdeltakelse = førsteTiltaksdeltakelse,
                )

                iverksettSøknadsbehandling(
                    tac = tac,
                    sakId = sak.id,
                    innvilgelsesperioder = innvilgelsesperioder(andreDeltakelsesperiode, andreTiltaksdeltakelse),
                    tiltaksdeltakelse = andreTiltaksdeltakelse,
                )

                val førsteTiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = førsteEksternId,
                    sakId = sak.id,
                    fom = førsteDeltakelseFom,
                    tom = førsteDeltakelsesTom,
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = førsteTiltaksdeltakerId,
                )
                val andreTiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                    id = andreEksternId,
                    sakId = sak.id,
                    fom = andreDeltakelseFom,
                    tom = andreDeltakelsesTom,
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = andreTiltaksdeltakerId,
                )

                tac.tiltaksdeltakerKafkaRepository.lagre(
                    førsteTiltaksdeltakerKafkaDb,
                    "melding",
                    nå(tac.clock).minusMinutes(20),
                )
                tac.tiltaksdeltakerKafkaRepository.lagre(
                    andreTiltaksdeltakerKafkaDb,
                    "melding",
                    nå(tac.clock).minusMinutes(20),
                )

                tac.endretTiltaksdeltakerJobb.opprettOppgaveEllerRevurderingForEndredeDeltakere()

                val førsteOppdatertTiltaksdeltakerKafkaDb =
                    tac.tiltaksdeltakerKafkaRepository.hent(førsteTiltaksdeltakerKafkaDb.id)
                førsteOppdatertTiltaksdeltakerKafkaDb shouldBe null

                val andreOppdatertTiltaksdeltakerKafkaDb =
                    tac.tiltaksdeltakerKafkaRepository.hent(andreTiltaksdeltakerKafkaDb.id)
                andreOppdatertTiltaksdeltakerKafkaDb shouldNotBe null
                andreOppdatertTiltaksdeltakerKafkaDb?.oppgaveId shouldBe null
            }
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ikke ferdigstilt - oppdaterer sist sjekket`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            tac.oppgaveFakeKlient.erFerdigstiltResponse = false

            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()
            val deltakelseFom = 5.januar(2025)
            val deltakelsesTom = 5.mai(2025)
            val deltakelsesperiode = deltakelseFom til deltakelsesTom

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(deltakelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                sakId = sak.id,
                fom = deltakelseFom,
                tom = LocalDate.now(tac.clock),
                deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                oppgaveId = oppgaveId,
                oppgaveSistSjekket = null,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerKafkaRepository.lagre(
                tiltaksdeltakerKafkaDb,
                "melding",
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.opprydning()

            val oppdatertTiltaksdeltakerKafkaDb = tac.tiltaksdeltakerKafkaRepository.hent(tiltaksdeltakerKafkaDb.id)
            oppdatertTiltaksdeltakerKafkaDb.shouldNotBeNull()
            oppdatertTiltaksdeltakerKafkaDb.oppgaveId shouldBe oppgaveId
            oppdatertTiltaksdeltakerKafkaDb.oppgaveSistSjekket?.truncatedTo(ChronoUnit.MINUTES) shouldBe nå(tac.clock).truncatedTo(
                ChronoUnit.MINUTES,
            )
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ferdigstilt - sletter fra db`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            tac.oppgaveFakeKlient.erFerdigstiltResponse = true

            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()
            val deltakelseFom = 5.januar(2025)
            val deltakelsesTom = 5.mai(2025)
            val deltakelsesperiode = deltakelseFom til deltakelsesTom

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(deltakelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val tiltaksdeltakerKafkaDb = getTiltaksdeltakerKafkaDb(
                sakId = sak.id,
                fom = deltakelseFom,
                tom = LocalDate.now(tac.clock),
                deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                oppgaveId = oppgaveId,
                oppgaveSistSjekket = nå(tac.clock).minusHours(2),
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerKafkaRepository.lagre(
                tiltaksdeltakerKafkaDb,
                "melding",
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.opprydning()

            tac.tiltaksdeltakerKafkaRepository.hent(tiltaksdeltakerKafkaDb.id) shouldBe null
        }
    }
}
