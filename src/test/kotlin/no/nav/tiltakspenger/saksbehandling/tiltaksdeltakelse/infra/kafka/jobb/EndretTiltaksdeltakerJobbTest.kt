package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppgaveId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.getTiltaksdeltakerHendelse
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class EndretTiltaksdeltakerJobbTest {
    private val oppgaveId = oppgaveId()

    @Test
    fun `hendelser innenfor forsinkelsesvinduet blir ikke behandlet`() {
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

            // Hendelse innenfor forsinkelsesvinduet (10 min siden, vindu er 15 min)
            val nyligHendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
                fom = deltakelseFom,
                tom = deltakelsesTom.minusDays(2),
                deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                nyligHendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(10),
            )

            tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

            // Hendelsen skal fortsatt være ubehandlet
            val ubehandlede = tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede()
            ubehandlede.any { it.id == nyligHendelse.id } shouldBe true

            val ubehandletHendelse = tac.tiltaksdeltakerHendelsePostgresRepo.hent(nyligHendelse.id)
            ubehandletHendelse.shouldNotBeNull()
            ubehandletHendelse.oppgaveId.shouldBeNull()
            ubehandletHendelse.behandlingId.shouldBeNull()

            // Saken skal ikke ha fått noen ny behandling
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger shouldHaveSize 1
        }
    }

    @Test
    fun `ingen opprettet behandling - ignorerer`() {
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

            val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )

            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                tiltaksdeltakerHendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

            tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede().none { it.id == tiltaksdeltakerHendelse.id } shouldBe true
        }
    }

    @Test
    fun `ingen behandling for endret deltaker - ignorerer`() {
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
            val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
            )

            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                tiltaksdeltakerHendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

            tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede().none { it.id == tiltaksdeltakerHendelse.id } shouldBe true
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

            val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
                deltakerstatus = TiltakDeltakerstatus.IkkeAktuell,
                fom = null,
                tom = null,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                tiltaksdeltakerHendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

            val oppdatertTiltaksdeltakerHendelse = tac.tiltaksdeltakerHendelsePostgresRepo.hent(tiltaksdeltakerHendelse.id)
            oppdatertTiltaksdeltakerHendelse.shouldNotBeNull()
            oppdatertTiltaksdeltakerHendelse.oppgaveId shouldBe oppgaveId
            oppdatertTiltaksdeltakerHendelse.behandlingId.shouldBeNull()
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

            val behandlingPaVent = behandling.settPåVent(kommando = kommando, clock = tac.clock).first as Søknadsbehandling
            tac.behandlingContext.rammebehandlingRepo.lagre(behandlingPaVent)

            val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
                deltakerstatus = TiltakDeltakerstatus.IkkeAktuell,
                fom = null,
                tom = null,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                tiltaksdeltakerHendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

            tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede().none { it.id == tiltaksdeltakerHendelse.id } shouldBe true
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).venterTil?.toLocalDate() shouldBe 1.mai(2025)
        }
    }

    @Test
    fun `iverksatt behandling, ingen endring - ignorerer`() {
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

            val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
                fom = deltakelseFom,
                tom = deltakelsesTom,
                dagerPerUke = 5F,
                deltakelsesprosent = 100F,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                tiltaksdeltakerHendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

            tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede().none { it.id == tiltaksdeltakerHendelse.id } shouldBe true
        }
    }

    @Test
    fun `iverksatt søknadsbehandling + revurdering innvilgelse - forlenget tom til samme som revurderingen - ignorerer`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()
            val deltakelseFom = 5.januar(2025)
            val opprinneligDeltakelsesTom = 5.mai(2025)
            val forlengetDeltakelsesTom = 5.juni(2025)
            val opprinneligDeltakelsesperiode = deltakelseFom til opprinneligDeltakelsesTom
            val forlengetDeltakelsesperiode = deltakelseFom til forlengetDeltakelsesTom

            val revurderingForlengelsePeriode = opprinneligDeltakelsesTom.plusDays(1) til forlengetDeltakelsesTom

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = opprinneligDeltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            // Opprett og iverksett søknadsbehandling med opprinnelig periode
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(opprinneligDeltakelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val tiltaksdeltakelseForlenget = tiltaksdeltakelse(
                periode = forlengetDeltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            tac.oppdaterTiltaksdeltakelse(fnr = sak.fnr, tiltaksdeltakelse = tiltaksdeltakelseForlenget)

            // Opprett og iverksett revurdering innvilgelse med forlenget tom
            iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(
                    revurderingForlengelsePeriode,
                    tiltaksdeltakelseForlenget,
                ),
            )

            // Opprett tiltaksdeltakerKafka med samme forlengede tom som revurderingen
            val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
                fom = deltakelseFom,
                tom = forlengetDeltakelsesTom,
                dagerPerUke = 5F,
                deltakelsesprosent = 100F,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                tiltaksdeltakerHendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

            tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede().none { it.id == tiltaksdeltakerHendelse.id } shouldBe true
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

            val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
                fom = deltakelseFom,
                tom = deltakelsesTom.plusMonths(1),
                tiltaksdeltakerId = tiltaksdeltakerId,
            )

            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                tiltaksdeltakerHendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

            val oppdatertTiltaksdeltakerHendelse = tac.tiltaksdeltakerHendelsePostgresRepo.hent(tiltaksdeltakerHendelse.id)

            val sisteBehandling = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last()

            oppdatertTiltaksdeltakerHendelse.shouldNotBeNull()
            oppdatertTiltaksdeltakerHendelse.oppgaveId.shouldBeNull()
            oppdatertTiltaksdeltakerHendelse.behandlingId shouldBe sisteBehandling.id

            val revurdering = sisteBehandling.shouldBeInstanceOf<Revurdering>()
            revurdering.id shouldBe oppdatertTiltaksdeltakerHendelse.behandlingId
            val grunn = revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
            grunn.hendelseId shouldBe tiltaksdeltakerHendelse.id.toString()
            grunn.endringer shouldHaveSize 2
            grunn.endringer.any { it is TiltaksdeltakerEndring.Forlengelse } shouldBe true
            grunn.endringer.any { it is TiltaksdeltakerEndring.EndretDeltakelsesmengde } shouldBe true
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

            val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
                fom = deltakelseFom,
                tom = deltakelsesTom.minusDays(2),
                deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                tiltaksdeltakerHendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(20),
            )

            tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

            val oppdatertTiltaksdeltakerHendelse = tac.tiltaksdeltakerHendelsePostgresRepo.hent(tiltaksdeltakerHendelse.id)
            oppdatertTiltaksdeltakerHendelse shouldNotBe null
            oppdatertTiltaksdeltakerHendelse?.oppgaveId shouldBe null

            val sisteBehandling = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last()
            val revurdering = sisteBehandling.shouldBeInstanceOf<Revurdering>()
            revurdering.id shouldBe oppdatertTiltaksdeltakerHendelse?.behandlingId
            val grunn = revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
            grunn.hendelseId shouldBe tiltaksdeltakerHendelse.id.toString()
            grunn.endringer shouldHaveSize 1
            grunn.endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.AvbruttDeltakelse>()
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

                val tiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                    sakId = sak.id,
                    fom = førsteDeltakelseFom,
                    tom = LocalDate.now(tac.clock),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = førsteTiltaksdeltakerId,
                )

                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    tiltaksdeltakerHendelse,
                    "melding",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(20),
                )

                tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

                tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede().none { it.id == tiltaksdeltakerHendelse.id } shouldBe true
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

                val førsteTiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                    id = førsteEksternId,
                    sakId = sak.id,
                    fom = førsteDeltakelseFom,
                    tom = LocalDate.now(tac.clock),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = førsteTiltaksdeltakerId,
                )
                val andreTiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                    id = andreEksternId,
                    sakId = sak.id,
                    fom = andreDeltakelseFom,
                    tom = LocalDate.now(tac.clock),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = andreTiltaksdeltakerId,
                )

                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    førsteTiltaksdeltakerHendelse,
                    "melding",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(20),
                )
                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    andreTiltaksdeltakerHendelse,
                    "melding",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(20),
                )

                tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

                val førsteOppdatertTiltaksdeltakerHendelse =
                    tac.tiltaksdeltakerHendelsePostgresRepo.hent(førsteTiltaksdeltakerHendelse.id)
                førsteOppdatertTiltaksdeltakerHendelse shouldNotBe null
                førsteOppdatertTiltaksdeltakerHendelse?.oppgaveId shouldBe null

                val sisteBehandling = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last()
                val revurdering = sisteBehandling.shouldBeInstanceOf<Revurdering>()
                revurdering.id shouldBe førsteOppdatertTiltaksdeltakerHendelse?.behandlingId
                val grunn = revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
                grunn.hendelseId shouldBe førsteTiltaksdeltakerHendelse.id.toString()
                grunn.endringer shouldHaveSize 1
                grunn.endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.AvbruttDeltakelse>()

                val andreOppdatertTiltaksdeltakerHendelse =
                    tac.tiltaksdeltakerHendelsePostgresRepo.hent(andreTiltaksdeltakerHendelse.id)
                andreOppdatertTiltaksdeltakerHendelse shouldNotBe null
                tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede().none { it.id == andreTiltaksdeltakerHendelse.id } shouldBe true
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

                val førsteTiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                    id = førsteEksternId,
                    sakId = sak.id,
                    fom = førsteDeltakelseFom,
                    tom = førsteDeltakelsesTom,
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = førsteTiltaksdeltakerId,
                )
                val andreTiltaksdeltakerHendelse = getTiltaksdeltakerHendelse(
                    id = andreEksternId,
                    sakId = sak.id,
                    fom = andreDeltakelseFom,
                    tom = andreDeltakelsesTom,
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = andreTiltaksdeltakerId,
                )

                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    førsteTiltaksdeltakerHendelse,
                    "melding",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(20),
                )
                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    andreTiltaksdeltakerHendelse,
                    "melding",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(20),
                )

                tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

                val førsteOppdatertTiltaksdeltakerHendelse =
                    tac.tiltaksdeltakerHendelsePostgresRepo.hent(førsteTiltaksdeltakerHendelse.id)
                førsteOppdatertTiltaksdeltakerHendelse shouldNotBe null
                tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede().none { it.id == førsteTiltaksdeltakerHendelse.id } shouldBe true

                val andreOppdatertTiltaksdeltakerHendelse =
                    tac.tiltaksdeltakerHendelsePostgresRepo.hent(andreTiltaksdeltakerHendelse.id)
                andreOppdatertTiltaksdeltakerHendelse shouldNotBe null
                andreOppdatertTiltaksdeltakerHendelse?.oppgaveId shouldBe null

                val sisteBehandling = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last()
                val revurdering = sisteBehandling.shouldBeInstanceOf<Revurdering>()
                revurdering.id shouldBe andreOppdatertTiltaksdeltakerHendelse?.behandlingId
                val grunn = revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
                grunn.hendelseId shouldBe andreTiltaksdeltakerHendelse.id.toString()
                grunn.endringer shouldHaveSize 1
                grunn.endringer.first().shouldBeInstanceOf<TiltaksdeltakerEndring.AvbruttDeltakelse>()
            }
        }
    }

    @Nested
    inner class `Flere hendelser for samme internDeltakerId` {

        @Test
        fun `kun nyeste hendelse evalueres, eldre hendelser markeres som behandlet`() {
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

                // Eldre hendelse: forlengelse
                val eldreHendelse = getTiltaksdeltakerHendelse(
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = deltakelsesTom.plusMonths(1),
                    tiltaksdeltakerId = tiltaksdeltakerId,
                )
                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    eldreHendelse,
                    "melding1",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(30),
                )

                // Nyeste hendelse: avbrutt
                val nyesteHendelse = getTiltaksdeltakerHendelse(
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = deltakelsesTom.minusDays(2),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = tiltaksdeltakerId,
                )
                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    nyesteHendelse,
                    "melding2",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(20),
                )

                tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

                // Eldre hendelse skal være markert som behandlet uten oppgave eller revurdering
                val oppdatertEldreHendelse = tac.tiltaksdeltakerHendelsePostgresRepo.hent(eldreHendelse.id)
                oppdatertEldreHendelse.shouldNotBeNull()
                oppdatertEldreHendelse.oppgaveId.shouldBeNull()
                oppdatertEldreHendelse.behandlingId.shouldBeNull()
                tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede().none { it.id == eldreHendelse.id } shouldBe true

                // Nyeste hendelse skal ha blitt evaluert og opprettet revurdering
                val oppdatertNyesteHendelse = tac.tiltaksdeltakerHendelsePostgresRepo.hent(nyesteHendelse.id)
                oppdatertNyesteHendelse.shouldNotBeNull()
                oppdatertNyesteHendelse.behandlingId.shouldNotBeNull()
                tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede().none { it.id == nyesteHendelse.id } shouldBe true

                val sisteBehandling = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last()
                val revurdering = sisteBehandling.shouldBeInstanceOf<Revurdering>()
                revurdering.id shouldBe oppdatertNyesteHendelse.behandlingId
                val grunn = revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
                grunn.hendelseId shouldBe nyesteHendelse.id.toString()
            }
        }

        @Test
        fun `tre hendelser for samme deltaker - kun siste evalueres, to eldre ignoreres`() {
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

                val eldsteHendelse = getTiltaksdeltakerHendelse(
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = deltakelsesTom.plusWeeks(1),
                    tiltaksdeltakerId = tiltaksdeltakerId,
                )
                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    eldsteHendelse,
                    "melding1",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(40),
                )

                val mellomHendelse = getTiltaksdeltakerHendelse(
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = deltakelsesTom.plusMonths(1),
                    tiltaksdeltakerId = tiltaksdeltakerId,
                )
                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    mellomHendelse,
                    "melding2",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(30),
                )

                val nyesteHendelse = getTiltaksdeltakerHendelse(
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = deltakelsesTom.plusMonths(2),
                    tiltaksdeltakerId = tiltaksdeltakerId,
                )
                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    nyesteHendelse,
                    "melding3",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(20),
                )

                tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

                val ubehandlede = tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede()

                // Alle tre skal være markert som behandlet
                ubehandlede.none { it.id == eldsteHendelse.id } shouldBe true
                ubehandlede.none { it.id == mellomHendelse.id } shouldBe true
                ubehandlede.none { it.id == nyesteHendelse.id } shouldBe true

                // De to eldste skal være ignorert (ingen oppgave eller behandling)
                val oppdatertEldste = tac.tiltaksdeltakerHendelsePostgresRepo.hent(eldsteHendelse.id)!!
                oppdatertEldste.oppgaveId.shouldBeNull()
                oppdatertEldste.behandlingId.shouldBeNull()

                val oppdatertMellom = tac.tiltaksdeltakerHendelsePostgresRepo.hent(mellomHendelse.id)!!
                oppdatertMellom.oppgaveId.shouldBeNull()
                oppdatertMellom.behandlingId.shouldBeNull()

                // Kun nyeste skal ha blitt evaluert og fått revurdering
                val oppdatertNyeste = tac.tiltaksdeltakerHendelsePostgresRepo.hent(nyesteHendelse.id)!!
                oppdatertNyeste.behandlingId.shouldNotBeNull()

                val sisteBehandling = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last()
                sisteBehandling.shouldBeInstanceOf<Revurdering>()
                sisteBehandling.id shouldBe oppdatertNyeste.behandlingId
            }
        }

        @Test
        fun `hendelser for ulike deltakere behandles uavhengig`() {
            withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
                val fnr = Fnr.random()
                val førsteTiltaksdeltakerId = TiltaksdeltakerId.random()
                val andreTiltaksdeltakerId = TiltaksdeltakerId.random()
                val førsteEksternId = UUID.randomUUID().toString()
                val andreEksternId = UUID.randomUUID().toString()
                val deltakelseFom = 5.januar(2025)
                val deltakelsesTom = 5.mai(2025)
                val deltakelsesperiode = deltakelseFom til deltakelsesTom

                val førsteTiltaksdeltakelse = tiltaksdeltakelse(
                    periode = deltakelsesperiode,
                    internDeltakelseId = førsteTiltaksdeltakerId,
                    eksternTiltaksdeltakelseId = førsteEksternId,
                )

                val andreDeltakelsesperiode = 10.mai(2025) til 11.juni(2025)
                val andreTiltaksdeltakelse = tiltaksdeltakelse(
                    periode = andreDeltakelsesperiode,
                    internDeltakelseId = andreTiltaksdeltakerId,
                    eksternTiltaksdeltakelseId = andreEksternId,
                )

                val (sak) = iverksettSøknadsbehandling(
                    tac = tac,
                    fnr = fnr,
                    innvilgelsesperioder = innvilgelsesperioder(deltakelsesperiode, førsteTiltaksdeltakelse),
                    tiltaksdeltakelse = førsteTiltaksdeltakelse,
                )

                iverksettSøknadsbehandling(
                    tac = tac,
                    sakId = sak.id,
                    innvilgelsesperioder = innvilgelsesperioder(andreDeltakelsesperiode, andreTiltaksdeltakelse),
                    tiltaksdeltakelse = andreTiltaksdeltakelse,
                )

                // To hendelser for første deltaker - eldre skal ignoreres
                val førsteEldreHendelse = getTiltaksdeltakerHendelse(
                    id = førsteEksternId,
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = deltakelsesTom.plusWeeks(1),
                    tiltaksdeltakerId = førsteTiltaksdeltakerId,
                )
                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    førsteEldreHendelse,
                    "melding1",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(30),
                )
                val førsteNyesteHendelse = getTiltaksdeltakerHendelse(
                    id = førsteEksternId,
                    sakId = sak.id,
                    fom = deltakelseFom,
                    tom = deltakelsesTom.plusMonths(1),
                    tiltaksdeltakerId = førsteTiltaksdeltakerId,
                )
                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    førsteNyesteHendelse,
                    "melding2",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(20),
                )

                // Én hendelse for andre deltaker - skal evalueres normalt
                val andreHendelse = getTiltaksdeltakerHendelse(
                    id = andreEksternId,
                    sakId = sak.id,
                    fom = andreDeltakelsesperiode.fraOgMed,
                    tom = andreDeltakelsesperiode.tilOgMed.minusDays(2),
                    deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                    tiltaksdeltakerId = andreTiltaksdeltakerId,
                )
                tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                    andreHendelse,
                    "melding3",
                    TiltaksdeltakerHendelseKilde.Komet,
                    nå(tac.clock).minusMinutes(20),
                )

                tac.endretTiltaksdeltakerJobb.håndterEndretTiltaksdeltakerHendelser()

                val ubehandlede = tac.tiltaksdeltakerHendelsePostgresRepo.hentUbehandlede()
                ubehandlede.none { it.id == førsteEldreHendelse.id } shouldBe true
                ubehandlede.none { it.id == førsteNyesteHendelse.id } shouldBe true
                ubehandlede.none { it.id == andreHendelse.id } shouldBe true

                // Eldre hendelse for første deltaker skal være ignorert
                val oppdatertFørsteEldre = tac.tiltaksdeltakerHendelsePostgresRepo.hent(førsteEldreHendelse.id)!!
                oppdatertFørsteEldre.oppgaveId.shouldBeNull()
                oppdatertFørsteEldre.behandlingId.shouldBeNull()

                // Nyeste hendelse for første deltaker skal ha blitt evaluert og fått revurdering
                val oppdatertFørsteNyeste = tac.tiltaksdeltakerHendelsePostgresRepo.hent(førsteNyesteHendelse.id)!!
                oppdatertFørsteNyeste.behandlingId.shouldNotBeNull()

                // Andre deltaker sin hendelse skal også ha blitt evaluert uavhengig
                // Men siden første deltaker sin revurdering nå er en åpen behandling, vil andre deltaker få oppgave i stedet
                val oppdatertAndre = tac.tiltaksdeltakerHendelsePostgresRepo.hent(andreHendelse.id)!!
                oppdatertAndre.oppgaveId.shouldNotBeNull()
            }
        }
    }
}
