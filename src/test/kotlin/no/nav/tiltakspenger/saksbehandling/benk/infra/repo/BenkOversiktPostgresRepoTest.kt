package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.domene.Sortering
import no.nav.tiltakspenger.saksbehandling.benk.domene.ÅpneBehandlingerFiltrering
import no.nav.tiltakspenger.saksbehandling.felles.Systembrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.repo.TestDataHelper
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAvbruttRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAvbruttSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterBrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandlingAvslag
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterKlarTilBehandlingManuellMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterKlarTilBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterManuellMeldekortBehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRevurderingUnderBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterUnderBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class BenkOversiktPostgresRepoTest {
    private fun newCommand(
        behandlingstype: List<BehandlingssammendragType>? = null,
        status: List<BehandlingssammendragStatus>? = null,
        saksbehandlere: List<String>? = null,
        sortering: Sortering = Sortering.ASC,
    ): HentÅpneBehandlingerCommand {
        return HentÅpneBehandlingerCommand(
            åpneBehandlingerFiltrering = ÅpneBehandlingerFiltrering(
                behandlingstype = behandlingstype,
                status = status,
                identer = saksbehandlere,
            ),
            sortering = sortering,
            saksbehandler = ObjectMother.saksbehandler(),
            correlationId = CorrelationId.generate(),
        )
    }

    @Test
    fun `henter åpne søknader uten behandling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val søknad = testDataHelper.persisterSakOgSøknad()
            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntall shouldBe 1
            actual.size shouldBe 1
            actual.first() shouldBe Behandlingssammendrag(
                sakId = søknad.sakId,
                fnr = søknad.fnr,
                saksnummer = søknad.saksnummer,
                startet = søknad.opprettet,
                kravtidspunkt = søknad.opprettet,
                behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = null,
                beslutter = null,
            )
        }
    }

    @Test
    fun `henter åpne søknadsbehandlinger`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sakOpprettetBehandling, opprettetBehandling) = testDataHelper.persisterOpprettetSøknadsbehandling()
            val (sakKlarTilBeslutning, klarTilBeslutning) = testDataHelper.persisterKlarTilBeslutningSøknadsbehandling(
                sakId = sakOpprettetBehandling.id,
                fnr = sakOpprettetBehandling.fnr,
                sak = sakOpprettetBehandling,
            )
            val (sakUnderBeslutning, underBeslutning) = testDataHelper.persisterUnderBeslutningSøknadsbehandling(
                sakId = sakKlarTilBeslutning.id,
                fnr = sakKlarTilBeslutning.fnr,
                sak = sakKlarTilBeslutning,
            )
            val (sakIverksatt) = testDataHelper.persisterIverksattSøknadsbehandling(
                sakId = sakUnderBeslutning.id,
                fnr = sakUnderBeslutning.fnr,
                sak = sakUnderBeslutning,
            )
            val (sakAvslag) = testDataHelper.persisterIverksattSøknadsbehandlingAvslag(
                sakId = sakIverksatt.id,
                fnr = sakIverksatt.fnr,
                sak = sakIverksatt,
            )
            testDataHelper.persisterAvbruttSøknadsbehandling(
                sakId = sakAvslag.id,
                fnr = sakAvslag.fnr,
                sak = sakAvslag,
            )

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntall shouldBe 3
            actual.size shouldBe 3
            actual.let {
                it.first() shouldBe Behandlingssammendrag(
                    sakId = sakOpprettetBehandling.id,
                    fnr = opprettetBehandling.fnr,
                    saksnummer = opprettetBehandling.saksnummer,
                    startet = opprettetBehandling.opprettet,
                    kravtidspunkt = opprettetBehandling.opprettet,
                    behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                    status = BehandlingssammendragStatus.UNDER_BEHANDLING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                )
                it[1] shouldBe Behandlingssammendrag(
                    sakId = sakKlarTilBeslutning.id,
                    fnr = klarTilBeslutning.fnr,
                    saksnummer = klarTilBeslutning.saksnummer,
                    startet = klarTilBeslutning.opprettet,
                    kravtidspunkt = klarTilBeslutning.opprettet,
                    behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                    status = BehandlingssammendragStatus.KLAR_TIL_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                )
                it.last() shouldBe Behandlingssammendrag(
                    sakId = sakUnderBeslutning.id,
                    fnr = underBeslutning.fnr,
                    saksnummer = underBeslutning.saksnummer,
                    startet = underBeslutning.opprettet,
                    kravtidspunkt = underBeslutning.opprettet,
                    behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                    status = BehandlingssammendragStatus.UNDER_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = ObjectMother.beslutter().navIdent,
                )
            }
        }
    }

    @Test
    fun `henter åpne revurderinger`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sakOpprettetRevurdering, opprettetRevurdering) = testDataHelper.persisterOpprettetRevurdering()
            val (sakRevurderingTilBeslutning, revurderingTilBeslutning) =
                testDataHelper.persisterRevurderingTilBeslutning(s = sakOpprettetRevurdering)
            val (sakMedRevurderingUnderBeslutning, revurderingUnderBeslutning) =
                testDataHelper.persisterRevurderingUnderBeslutning(sakRevurderingTilBeslutning)

            testDataHelper.persisterIverksattRevurdering(sak = sakMedRevurderingUnderBeslutning)
            testDataHelper.persisterAvbruttRevurdering(sak = sakMedRevurderingUnderBeslutning)

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntall shouldBe 3
            actual.size shouldBe 3
            actual.let {
                it.first() shouldBe Behandlingssammendrag(
                    sakId = sakOpprettetRevurdering.id,
                    fnr = opprettetRevurdering.fnr,
                    saksnummer = opprettetRevurdering.saksnummer,
                    startet = opprettetRevurdering.opprettet,
                    behandlingstype = BehandlingssammendragType.REVURDERING,
                    status = BehandlingssammendragStatus.UNDER_BEHANDLING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                    kravtidspunkt = null,
                )
                it[1] shouldBe Behandlingssammendrag(
                    sakId = sakRevurderingTilBeslutning.id,
                    fnr = revurderingTilBeslutning.fnr,
                    saksnummer = revurderingTilBeslutning.saksnummer,
                    startet = revurderingTilBeslutning.opprettet,
                    behandlingstype = BehandlingssammendragType.REVURDERING,
                    status = BehandlingssammendragStatus.KLAR_TIL_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                    kravtidspunkt = null,
                )
                it.last() shouldBe Behandlingssammendrag(
                    sakId = sakMedRevurderingUnderBeslutning.id,
                    fnr = revurderingUnderBeslutning.fnr,
                    saksnummer = revurderingUnderBeslutning.saksnummer,
                    startet = revurderingUnderBeslutning.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.REVURDERING,
                    status = BehandlingssammendragStatus.UNDER_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = ObjectMother.beslutter().navIdent,
                )
            }
        }
    }

    @Test
    fun `henter åpne meldekortbehandlinger`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sakMedInnsendtBrukersMeldekort, brukersMeldekort) = testDataHelper.persisterBrukersMeldekort()
            val (sakMedOpprettetMeldekortBehandling, opprettetMeldekortbehandling) = testDataHelper.persisterKlarTilBehandlingManuellMeldekortBehandling()
            val (sakMedMeldekortbehandlingTilBeslutning, meldekortbehandlingTilBeslutning) = testDataHelper.persisterManuellMeldekortBehandlingTilBeslutning()
            testDataHelper.persisterIverksattMeldekortbehandling()

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntall shouldBe 3
            actual.size shouldBe 3
            testDataHelper.verifiserViHar3MeldekortBehandlinger()

            actual.let {
                it.first() shouldBe Behandlingssammendrag(
                    sakId = sakMedInnsendtBrukersMeldekort.id,
                    fnr = sakMedInnsendtBrukersMeldekort.fnr,
                    saksnummer = sakMedInnsendtBrukersMeldekort.saksnummer,
                    startet = brukersMeldekort.mottatt,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.INNSENDT_MELDEKORT,
                    status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                    saksbehandler = null,
                    beslutter = null,
                )
                it[1] shouldBe Behandlingssammendrag(
                    sakId = sakMedOpprettetMeldekortBehandling.id,
                    fnr = sakMedOpprettetMeldekortBehandling.fnr,
                    saksnummer = sakMedOpprettetMeldekortBehandling.saksnummer,
                    startet = opprettetMeldekortbehandling.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                    status = BehandlingssammendragStatus.UNDER_BEHANDLING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                )
                it.last() shouldBe Behandlingssammendrag(
                    sakId = sakMedMeldekortbehandlingTilBeslutning.id,
                    fnr = sakMedMeldekortbehandlingTilBeslutning.fnr,
                    saksnummer = sakMedMeldekortbehandlingTilBeslutning.saksnummer,
                    startet = meldekortbehandlingTilBeslutning.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                    status = BehandlingssammendragStatus.KLAR_TIL_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                )
            }
        }
    }

    @Test
    fun `henter mix av behandlingene`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            testDataHelper.persisterSakOgSøknad()
            testDataHelper.persisterOpprettetSøknadsbehandling()
            testDataHelper.persisterOpprettetRevurdering()
            testDataHelper.persisterKlarTilBehandlingManuellMeldekortBehandling()

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntall shouldBe 4
            actual.size shouldBe 4
        }
    }

    @Test
    fun `kan filtrere basert på behandlingstype`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            testDataHelper.persisterSakOgSøknad()
            testDataHelper.persisterOpprettetSøknadsbehandling()
            testDataHelper.persisterOpprettetRevurdering()
            testDataHelper.persisterKlarTilBehandlingManuellMeldekortBehandling()

            val (actualSøknadsbehandlinger, totalAntallSøknadbehandlinger) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(behandlingstype = listOf(BehandlingssammendragType.SØKNADSBEHANDLING)),
            )
            val (actualRevurderinger, totalAntallRevurderinger) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(behandlingstype = listOf(BehandlingssammendragType.REVURDERING)),
            )
            val (actualMeldekortBehandlinger, totalAntallMeldekortbehandlinger) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(behandlingstype = listOf(BehandlingssammendragType.MELDEKORTBEHANDLING)),
            )

            actualSøknadsbehandlinger.size shouldBe 2
            totalAntallSøknadbehandlinger shouldBe 2
            actualRevurderinger.size shouldBe 1
            totalAntallRevurderinger shouldBe 1
            actualMeldekortBehandlinger.size shouldBe 1
            totalAntallMeldekortbehandlinger shouldBe 1
        }
    }

    @Test
    fun `kan filtrere basert på status`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            testDataHelper.persisterSakOgSøknad()
            testDataHelper.persisterOpprettetSøknadsbehandling()
            testDataHelper.persisterKlarTilBeslutningSøknadsbehandling()
            testDataHelper.persisterUnderBeslutningSøknadsbehandling()

            testDataHelper.persisterOpprettetRevurdering()
            testDataHelper.persisterRevurderingTilBeslutning()
            testDataHelper.persisterRevurderingUnderBeslutning()

            testDataHelper.persisterKlarTilBehandlingManuellMeldekortBehandling()
            testDataHelper.persisterManuellMeldekortBehandlingTilBeslutning()

            val (actualKlarTilBehandling, _) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(status = listOf(BehandlingssammendragStatus.KLAR_TIL_BEHANDLING)),
            )

            val (actualUnderBehandling, _) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(status = listOf(BehandlingssammendragStatus.UNDER_BEHANDLING)),
            )

            val (actualKlarTilBeslutning, _) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(status = listOf(BehandlingssammendragStatus.KLAR_TIL_BESLUTNING)),
            )

            val (actualUnderBeslutning, _) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(status = listOf(BehandlingssammendragStatus.UNDER_BESLUTNING)),
            )

            actualKlarTilBehandling.size shouldBe 1
            actualUnderBehandling.size shouldBe 3
            actualKlarTilBeslutning.size shouldBe 3
            actualUnderBeslutning.size shouldBe 2
        }
    }

    @Test
    fun `kan filtrere basert på saksbehandler og beslutter`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            @Suppress("UNCHECKED_CAST")
            val saksbehandler = Saksbehandler(
                navIdent = "ocurreret",
                brukernavn = "dissentiunt",
                epost = "hac",
                roller = Saksbehandlerroller(setOf(Saksbehandlerrolle.SAKSBEHANDLER, Saksbehandlerrolle.BESLUTTER)),
                scopes = Systembrukerroller(emptySet()) as GenerellSystembrukerroller<GenerellSystembrukerrolle>,
                klientId = "persius",
                klientnavn = "possim",
            )
            testDataHelper.persisterOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
            testDataHelper.persisterKlarTilBeslutningSøknadsbehandling()
            testDataHelper.persisterUnderBeslutningSøknadsbehandling(beslutter = saksbehandler)

            val (behandlingssamendrag, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(saksbehandlere = listOf(saksbehandler.navIdent)),
            )

            totalAntall shouldBe 2
            behandlingssamendrag.size shouldBe 2
            behandlingssamendrag.let {
                it.first().saksbehandler shouldBe saksbehandler.navIdent
                it.first().beslutter shouldBe null

                it.last().saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
                it.last().beslutter shouldBe saksbehandler.navIdent
            }
        }
    }

    @Test
    fun `henter behandlinger som har saksbehandler eller beslutter ikke tildelt`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            @Suppress("UNCHECKED_CAST")
            val saksbehandler = Saksbehandler(
                navIdent = "ocurreret",
                brukernavn = "dissentiunt",
                epost = "hac",
                roller = Saksbehandlerroller(setOf(Saksbehandlerrolle.SAKSBEHANDLER, Saksbehandlerrolle.BESLUTTER)),
                scopes = Systembrukerroller(emptySet()) as GenerellSystembrukerroller<GenerellSystembrukerrolle>,
                klientId = "persius",
                klientnavn = "possim",
            )

            testDataHelper.persisterOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
            testDataHelper.persisterKlarTilBeslutningSøknadsbehandling()
            testDataHelper.persisterUnderBeslutningSøknadsbehandling(beslutter = saksbehandler)

            val (behandlingssamendrag, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(saksbehandlere = listOf("IKKE_TILDELT")),
            )

            totalAntall shouldBe 2
            behandlingssamendrag.size shouldBe 2
            behandlingssamendrag.let {
                it.first().saksbehandler shouldBe saksbehandler.navIdent
                it.first().beslutter shouldBe null

                it.last().saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
                it.last().beslutter shouldBe null
            }
        }
    }

    @Test
    fun `kan sortere asc og desc`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val søknad = testDataHelper.persisterSakOgSøknad()
            val (sak2, _) = testDataHelper.persisterOpprettetSøknadsbehandling()

            val (actualAsc, _) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())
            val (actualDesc, _) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand(sortering = Sortering.DESC))

            actualAsc.size shouldBe 2
            actualAsc.let {
                it.first().sakId shouldBe søknad.sakId
                it.last().sakId shouldBe sak2.id
            }

            actualDesc.size shouldBe 2
            actualDesc.let {
                it.first().sakId shouldBe sak2.id
                it.last().sakId shouldBe søknad.sakId
            }
        }
    }
}

private fun TestDataHelper.verifiserViHar3MeldekortBehandlinger() {
    sessionFactory.withSession { session ->
        session.run(
            queryOf("SELECT COUNT(*) FROM meldekortbehandling", emptyMap()).map {
                it.int(1)
            }.asSingle,
        ) shouldBe 3
    }
}
