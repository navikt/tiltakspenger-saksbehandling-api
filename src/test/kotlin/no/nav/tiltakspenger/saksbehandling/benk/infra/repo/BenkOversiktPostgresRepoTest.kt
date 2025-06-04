package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.infra.repo.TestDataHelper
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAvbruttRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAvbruttSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandlingAvslag
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterKlarTilBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterManuellMeldekortBehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetManuellMeldekortBehandling
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
    @Test
    fun `henter åpne søknader uten behandling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val søknad = testDataHelper.persisterSakOgSøknad()
            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger()

            totalAntall shouldBe 1
            actual.size shouldBe 1
            actual.first() shouldBe Behandlingssammendrag(
                sakId = søknad.sakId,
                fnr = søknad.fnr,
                saksnummer = søknad.saksnummer,
                startet = søknad.opprettet,
                kravtidspunkt = søknad.opprettet,
                behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                status = null,
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

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger()

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

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger()

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
            val (sakMedOpprettetMeldekortBehandling, opprettetMeldekortbehandling) = testDataHelper.persisterOpprettetManuellMeldekortBehandling()
            val (sakMedMeldekortbehandlingTilBeslutning, meldekortbehandlingTilBeslutning) = testDataHelper.persisterManuellMeldekortBehandlingTilBeslutning()
            testDataHelper.persisterIverksattMeldekortbehandling()

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger()

            totalAntall shouldBe 2
            actual.size shouldBe 2
            testDataHelper.verifiserViHar3MeldekortBehandlinger()

            actual.let {
                it.first() shouldBe Behandlingssammendrag(
                    sakId = sakMedOpprettetMeldekortBehandling.id,
                    fnr = sakMedOpprettetMeldekortBehandling.fnr,
                    saksnummer = sakMedOpprettetMeldekortBehandling.saksnummer,
                    startet = opprettetMeldekortbehandling.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                    status = BehandlingssammendragStatus.KLAR_TIL_UTFYLLING,
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
            testDataHelper.persisterOpprettetManuellMeldekortBehandling()

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger()

            totalAntall shouldBe 4
            actual.size shouldBe 4
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
