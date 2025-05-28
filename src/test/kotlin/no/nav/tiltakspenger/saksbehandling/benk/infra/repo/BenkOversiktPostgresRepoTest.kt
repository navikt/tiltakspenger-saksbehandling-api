package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAvbruttRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAvbruttSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandlingAvslag
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterKlarTilBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRevurderingUnderBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterUnderBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class BenkOversiktPostgresRepoTest {
    @Test
    fun `henter åpne søknader uten behandling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val søknad = testDataHelper.persisterSakOgSøknad()
            val actual = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger()

            actual.size shouldBe 1
            actual.first() shouldBe Behandlingssammendrag(
                fnr = søknad.fnr,
                saksnummer = søknad.saksnummer,
                startet = søknad.opprettet,
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

            val actual = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger()

            actual.size shouldBe 3
            actual.let {
                it.first() shouldBe Behandlingssammendrag(
                    fnr = opprettetBehandling.fnr,
                    saksnummer = opprettetBehandling.saksnummer,
                    startet = opprettetBehandling.opprettet,
                    behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                    status = Behandlingsstatus.UNDER_BEHANDLING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                )
                it[1] shouldBe Behandlingssammendrag(
                    fnr = klarTilBeslutning.fnr,
                    saksnummer = klarTilBeslutning.saksnummer,
                    startet = klarTilBeslutning.opprettet,
                    behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                    status = Behandlingsstatus.KLAR_TIL_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                )
                it.last() shouldBe Behandlingssammendrag(
                    fnr = underBeslutning.fnr,
                    saksnummer = underBeslutning.saksnummer,
                    startet = underBeslutning.opprettet,
                    behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                    status = Behandlingsstatus.UNDER_BESLUTNING,
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

            val actual = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger()

            actual.size shouldBe 3
            actual.let {
                it.first() shouldBe Behandlingssammendrag(
                    fnr = opprettetRevurdering.fnr,
                    saksnummer = opprettetRevurdering.saksnummer,
                    startet = opprettetRevurdering.opprettet,
                    behandlingstype = BehandlingssammendragType.REVURDERING,
                    status = Behandlingsstatus.UNDER_BEHANDLING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                )
                it[1] shouldBe Behandlingssammendrag(
                    fnr = revurderingTilBeslutning.fnr,
                    saksnummer = revurderingTilBeslutning.saksnummer,
                    startet = revurderingTilBeslutning.opprettet,
                    behandlingstype = BehandlingssammendragType.REVURDERING,
                    status = Behandlingsstatus.KLAR_TIL_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                )
                it.last() shouldBe Behandlingssammendrag(
                    fnr = revurderingUnderBeslutning.fnr,
                    saksnummer = revurderingUnderBeslutning.saksnummer,
                    startet = revurderingUnderBeslutning.opprettet,
                    behandlingstype = BehandlingssammendragType.REVURDERING,
                    status = Behandlingsstatus.UNDER_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = ObjectMother.beslutter().navIdent,
                )
            }
        }
    }

    @Test
    fun `henter åpne meldekortbehandlinger`() {
        fail("Ikke implementert")
    }

    @Test
    fun `henter mix av behandlingene`() {
        fail("Ikke implementert")
    }
}
