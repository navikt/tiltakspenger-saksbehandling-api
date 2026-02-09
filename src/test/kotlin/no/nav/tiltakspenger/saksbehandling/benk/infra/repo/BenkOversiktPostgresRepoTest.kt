package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragBenktype
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSortering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.domene.SorteringRetning
import no.nav.tiltakspenger.saksbehandling.benk.domene.ÅpneBehandlingerFiltrering
import no.nav.tiltakspenger.saksbehandling.felles.Systembrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.repo.TestDataHelper
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAvbruttRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAvbruttSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterAvsluttetMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterBrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattRevurderingStans
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandlingAvslag
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterKlarTilBehandlingManuellMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterKlarTilBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterManuellMeldekortBehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOppdatertMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRevurderingStansTilBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRevurderingStansUnderBeslutning
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterUnderBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.Clock

class BenkOversiktPostgresRepoTest {
    private fun newCommand(
        benktype: List<BehandlingssammendragBenktype>? = null,
        behandlingstype: List<BehandlingssammendragType>? = null,
        status: List<BehandlingssammendragStatus>? = null,
        saksbehandlere: List<String>? = null,
        sortering: BenkSortering = BenkSortering(BenkSorteringKolonne.STARTET, SorteringRetning.ASC),
    ): HentÅpneBehandlingerCommand {
        return HentÅpneBehandlingerCommand(
            åpneBehandlingerFiltrering = ÅpneBehandlingerFiltrering(
                benktype = benktype,
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
                erSattPåVent = false,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                sistEndret = null,
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
                    sistEndret = opprettetBehandling.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
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
                    sistEndret = klarTilBeslutning.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
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
                    sistEndret = underBeslutning.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                )
            }
        }
    }

    @Test
    fun `henter åpne revurderinger`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sakOpprettetRevurdering, opprettetRevurdering) = testDataHelper.persisterOpprettetRevurdering()
            val (sakRevurderingTilBeslutning, revurderingTilBeslutning) =
                testDataHelper.persisterRevurderingStansTilBeslutning(s = sakOpprettetRevurdering)
            val (sakMedRevurderingUnderBeslutning, revurderingUnderBeslutning) =
                testDataHelper.persisterRevurderingStansUnderBeslutning(sakRevurderingTilBeslutning)

            testDataHelper.persisterIverksattRevurderingStans(sak = sakMedRevurderingUnderBeslutning)
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
                    sistEndret = opprettetRevurdering.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
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
                    sistEndret = revurderingTilBeslutning.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
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
                    sistEndret = revurderingUnderBeslutning.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                )
            }
        }
    }

    @Test
    fun `henter meldekort som er klar til behandling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sak1, _) = testDataHelper.persisterBrukersMeldekort(periode = Periode(2.januar(2023), 29.januar(2023)))
            val (sak1MedKorrigering, andreMeldekortSak1) = testDataHelper.persisterBrukersMeldekort(
                sak = sak1,
                meldeperiode = sak1.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
            )
            val (sak1MedMeldekortForEnAnnenPeriode, tredjeMeldekortSak1) = testDataHelper.persisterBrukersMeldekort(
                sak = sak1MedKorrigering,
                meldeperiode = sak1.meldeperiodeKjeder.last().hentSisteMeldeperiode(),
            )
            val (sak2, førsteMeldekortSak2) = testDataHelper.persisterBrukersMeldekort()

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntall shouldBe 3
            actual.size shouldBe 3

            actual.let {
                it.first() shouldBe Behandlingssammendrag(
                    sakId = sak1MedKorrigering.id,
                    fnr = sak1MedKorrigering.fnr,
                    saksnummer = sak1MedKorrigering.saksnummer,
                    startet = andreMeldekortSak1.mottatt,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.KORRIGERT_MELDEKORT,
                    status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                    saksbehandler = null,
                    beslutter = null,
                    sistEndret = null,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                )
                it[1] shouldBe Behandlingssammendrag(
                    sakId = sak1MedMeldekortForEnAnnenPeriode.id,
                    fnr = sak1MedMeldekortForEnAnnenPeriode.fnr,
                    saksnummer = sak1MedMeldekortForEnAnnenPeriode.saksnummer,
                    startet = tredjeMeldekortSak1.mottatt,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.INNSENDT_MELDEKORT,
                    status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                    saksbehandler = null,
                    beslutter = null,
                    sistEndret = null,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                )
                it.last() shouldBe Behandlingssammendrag(
                    sakId = sak2.id,
                    fnr = sak2.fnr,
                    saksnummer = sak2.saksnummer,
                    startet = førsteMeldekortSak2.mottatt,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.INNSENDT_MELDEKORT,
                    status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                    saksbehandler = null,
                    beslutter = null,
                    sistEndret = null,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
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
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    sistEndret = null,
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
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    sistEndret = opprettetMeldekortbehandling.sistEndret,
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
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    sistEndret = meldekortbehandlingTilBeslutning.sistEndret,
                )
            }
        }
    }

    @Test
    fun `henter ikke meldekort som har mottatt tidspunkt som er mindre enn siste meldekort behandling`() {
        val clock = TikkendeKlokke(fixedClockAt(18.august(2025)))
        withMigratedDb(runIsolated = true, clock = clock) { testDataHelper ->
            val periode = Periode(4.august(2025), 17.august(2025))
            val (sakMedInnsendtBrukersMeldekort, brukersMeldekort) = testDataHelper.persisterBrukersMeldekort(
                periode = periode,
            )
            val (sakMedIverksattMeldekortBehandling, _) = testDataHelper.persisterIverksattMeldekortbehandling(
                sak = sakMedInnsendtBrukersMeldekort,
                periode = brukersMeldekort.periode,
            )
            val (sakMedKorrigertMeldekort, korrigertMeldekort) = testDataHelper.persisterBrukersMeldekort(
                sak = sakMedIverksattMeldekortBehandling,
                periode = periode,
            )

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntall shouldBe 1
            actual shouldBe listOf(
                Behandlingssammendrag(
                    sakId = sakMedKorrigertMeldekort.id,
                    fnr = sakMedKorrigertMeldekort.fnr,
                    saksnummer = sakMedKorrigertMeldekort.saksnummer,
                    startet = korrigertMeldekort.mottatt,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.KORRIGERT_MELDEKORT,
                    status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                    saksbehandler = null,
                    beslutter = null,
                    sistEndret = null,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                ),
            )

            testDataHelper.persisterIverksattMeldekortbehandling(
                sak = sakMedInnsendtBrukersMeldekort,
                periode = brukersMeldekort.periode,
            )

            val (actualEtterIverksettingIgjen, totalAntallEtterIverksettingIgjen) =
                testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntallEtterIverksettingIgjen shouldBe 0
            actualEtterIverksettingIgjen shouldBe emptyList()
        }
    }

    @Test
    fun `henter ikke meldekort der en behandling i ettertid har blitt avsluttet`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val periode = Periode(4.august(2025), 17.august(2025))
            val (sakMedInnsendtBrukersMeldekort, brukersMeldekort) = testDataHelper.persisterBrukersMeldekort(
                periode = periode,
            )

            val (actualFørBehandling, totalAntallFørBehandling) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )
            totalAntallFørBehandling shouldBe 1
            actualFørBehandling shouldBe listOf(
                Behandlingssammendrag(
                    sakId = sakMedInnsendtBrukersMeldekort.id,
                    fnr = sakMedInnsendtBrukersMeldekort.fnr,
                    saksnummer = sakMedInnsendtBrukersMeldekort.saksnummer,
                    startet = brukersMeldekort.mottatt,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.INNSENDT_MELDEKORT,
                    status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                    saksbehandler = null,
                    beslutter = null,
                    sistEndret = null,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                ),
            )

            val (sakEtterBehandling, behandling) = testDataHelper.persisterKlarTilBehandlingManuellMeldekortBehandling(
                sak = sakMedInnsendtBrukersMeldekort,
                periode = brukersMeldekort.periode,
                kjedeId = brukersMeldekort.kjedeId,
            )

            val (actualEtterBehandling, totalAntallEtterBehandling) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )
            totalAntallEtterBehandling shouldBe 1
            actualEtterBehandling shouldBe listOf(
                Behandlingssammendrag(
                    sakId = sakEtterBehandling.id,
                    fnr = sakEtterBehandling.fnr,
                    saksnummer = sakEtterBehandling.saksnummer,
                    startet = behandling.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                    status = BehandlingssammendragStatus.UNDER_BEHANDLING,
                    saksbehandler = behandling.saksbehandler,
                    beslutter = null,
                    sistEndret = behandling.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                ),
            )

            testDataHelper.persisterAvsluttetMeldekortBehandling(sak = sakEtterBehandling, periode = behandling.periode)

            val (actualEtterAvbrytelse, totalAntallEtterAvbrytelse) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntallEtterAvbrytelse shouldBe 0
            actualEtterAvbrytelse shouldBe emptyList()
        }
    }

    @Test
    fun `henter ikke meldekort der en behandling ble endret etter at meldekortet var mottatt`() {
        val clock = TikkendeKlokke(fixedClockAt(18.august(2025)))
        withMigratedDb(runIsolated = true, clock = clock) { testDataHelper ->
            val periode = Periode(4.august(2025), 17.august(2025))
            val (sakMedInnsendtBrukersMeldekort, brukersMeldekort) = testDataHelper.persisterBrukersMeldekort(
                periode = periode,
            )

            val (actualMedNyttMeldekort, totalAntallMedNyttMeldekort) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntallMedNyttMeldekort shouldBe 1
            actualMedNyttMeldekort.single().behandlingstype shouldBe BehandlingssammendragType.INNSENDT_MELDEKORT

            val (sakEtterBehandling, behandling) = testDataHelper.persisterKlarTilBehandlingManuellMeldekortBehandling(
                sak = sakMedInnsendtBrukersMeldekort,
                periode = brukersMeldekort.periode,
                kjedeId = brukersMeldekort.kjedeId,
            )

            val (actualMedNyBehandling, totalAntallMedNyBehandling) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntallMedNyBehandling shouldBe 1
            actualMedNyBehandling.single().behandlingstype shouldBe BehandlingssammendragType.MELDEKORTBEHANDLING

            // Bruker sender en korrigering
            testDataHelper.persisterBrukersMeldekort(
                sak = sakEtterBehandling,
                periode = periode,
            )

            val (actualMedKorrigeringFraBruker, totalAntallMedNyKorrigeringFraBruker) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            // Korrigeringen skal vises inntil meldekortbehandlingen er oppdatert
            totalAntallMedNyKorrigeringFraBruker shouldBe 2
            actualMedKorrigeringFraBruker[0].behandlingstype shouldBe BehandlingssammendragType.MELDEKORTBEHANDLING
            actualMedKorrigeringFraBruker[1].behandlingstype shouldBe BehandlingssammendragType.KORRIGERT_MELDEKORT

            testDataHelper.persisterOppdatertMeldekortbehandling(behandling = behandling)

            val (actualMedOppdatertBehandling, totalAntallMedOppdatertBehandling) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntallMedOppdatertBehandling shouldBe 1
            actualMedOppdatertBehandling.single().behandlingstype shouldBe BehandlingssammendragType.MELDEKORTBEHANDLING
        }
    }

    @Test
    fun `henter åpne klagebehandlinger`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (_, klagebehandling) = testDataHelper.persisterOpprettetKlagebehandlingTilAvvisning()

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntall shouldBe 1
            actual.size shouldBe 1
            actual.first() shouldBe Behandlingssammendrag(
                sakId = klagebehandling.sakId,
                fnr = klagebehandling.fnr,
                saksnummer = klagebehandling.saksnummer,
                startet = klagebehandling.opprettet,
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.KLAGEBEHANDLING,
                status = BehandlingssammendragStatus.UNDER_BEHANDLING,
                saksbehandler = ObjectMother.saksbehandler().navIdent,
                beslutter = null,
                erSattPåVent = false,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                sistEndret = klagebehandling.sistEndret,
            )
        }
    }

    @Test
    fun `henter mix av behandlingene`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            testDataHelper.persisterSakOgSøknad()
            testDataHelper.persisterOpprettetSøknadsbehandling()
            testDataHelper.persisterOpprettetRevurdering()
            testDataHelper.persisterKlarTilBehandlingManuellMeldekortBehandling()
            testDataHelper.persisterOpprettetKlagebehandlingTilAvvisning()

            val (actual, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntall shouldBe 5
            actual.size shouldBe 5
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
            testDataHelper.persisterRevurderingStansTilBeslutning()
            testDataHelper.persisterRevurderingStansUnderBeslutning()

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
    fun `henter både behandlinger som er klar og på vent`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val beslutter = ObjectMother.beslutter("Z111111")

            testDataHelper.persisterUnderBeslutningSøknadsbehandling(beslutter = beslutter)
            val (_, behandling) = testDataHelper.persisterUnderBeslutningSøknadsbehandling(beslutter = beslutter)
            val kommando = SettRammebehandlingPåVentKommando(
                sakId = behandling.sakId,
                rammebehandlingId = behandling.id,
                begrunnelse = "Venter på AAP søknad",
                saksbehandler = beslutter,
                venterTil = null,
            )
            val oppdatertBehandling = behandling.settPåVent(kommando, testDataHelper.clock)
            testDataHelper.behandlingRepo.lagre(oppdatertBehandling)

            val (behandlingssamendrag, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntall shouldBe 2
            behandlingssamendrag.size shouldBe 2
        }
    }

    @Test
    fun `henter både behandlinger som er klar`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val beslutter = ObjectMother.beslutter("Z111111")

            testDataHelper.persisterUnderBeslutningSøknadsbehandling(beslutter = beslutter)
            val (_, behandling) = testDataHelper.persisterUnderBeslutningSøknadsbehandling(beslutter = beslutter)
            val kommando = SettRammebehandlingPåVentKommando(
                sakId = behandling.sakId,
                rammebehandlingId = behandling.id,
                begrunnelse = "Venter på AAP søknad",
                saksbehandler = beslutter,
            )
            val oppdatertBehandling = behandling.settPåVent(kommando, testDataHelper.clock)
            testDataHelper.behandlingRepo.lagre(oppdatertBehandling)

            val (behandlingssamendrag, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(benktype = listOf(BehandlingssammendragBenktype.KLAR)),
            )

            totalAntall shouldBe 1
            behandlingssamendrag.size shouldBe 1
        }
    }

    @Test
    fun `kan filtrere på behandlinger som er satt på vent`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val beslutter = ObjectMother.beslutter("Z111111")

            testDataHelper.persisterUnderBeslutningSøknadsbehandling(beslutter = beslutter)
            val (_, behandling) = testDataHelper.persisterUnderBeslutningSøknadsbehandling(beslutter = beslutter)
            val kommando = SettRammebehandlingPåVentKommando(
                sakId = behandling.sakId,
                rammebehandlingId = behandling.id,
                begrunnelse = "Venter på AAP søknad",
                saksbehandler = beslutter,
            )
            val oppdatertBehandling = behandling.settPåVent(kommando, Clock.system(zoneIdOslo))
            testDataHelper.behandlingRepo.lagre(oppdatertBehandling)

            val (behandlingssamendrag, totalAntall) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(benktype = listOf(BehandlingssammendragBenktype.VENTER)),
            )

            totalAntall shouldBe 1
            behandlingssamendrag.size shouldBe 1
        }
    }

    @Test
    fun `kan sortere asc og desc`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val søknad = testDataHelper.persisterSakOgSøknad()
            val (sak2, _) = testDataHelper.persisterOpprettetSøknadsbehandling()

            val (actualAsc, _) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(sortering = BenkSortering(BenkSorteringKolonne.STARTET, SorteringRetning.ASC)),
            )
            val (actualDesc, _) = testDataHelper.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(sortering = BenkSortering(BenkSorteringKolonne.STARTET, SorteringRetning.DESC)),
            )

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
