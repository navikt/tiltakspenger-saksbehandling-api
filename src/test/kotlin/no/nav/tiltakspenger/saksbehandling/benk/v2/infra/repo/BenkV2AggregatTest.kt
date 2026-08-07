package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.repo

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlagebehandlingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortType
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadstype
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKilde
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingStatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Filtrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Sortering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2SorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2SorteringRetning
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.HentBenkV2Command
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort.Companion.MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilOpprettholdelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgSettPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilGodkjenning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjenn
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Aggregat-test for benk v2, jf. testtaksonomien i `AGENTS.md`.
 *
 * Benken er nettopp en spørring på tvers av alle saker, så totalantallet er featuren og ikke en krykke.
 * Derfor kjører hver test isolert, og derfor kalles `hent*(limit)` direkte.
 *
 * Tilstanden bygges gjennom prodstiene: routene, konsumentene og jobbene, slik de kjører i nais.
 */
class BenkV2AggregatTest {

    private fun <F : BenkV2Filtrering, K : BenkV2SorteringKolonne> command(
        filtrering: F,
        kolonne: K,
        retning: BenkV2SorteringRetning = BenkV2SorteringRetning.ASC,
    ) = HentBenkV2Command(
        filtrering = filtrering,
        sortering = BenkV2Sortering(kolonne, retning),
        saksbehandler = ObjectMother.saksbehandler(),
        correlationId = CorrelationId.generate(),
    )

    private fun søknaderCommand(
        status: BenkV2Behandlingsstatus? = null,
        søknadstype: BenkSøknadstype? = null,
        saksbehandler: String? = null,
        kolonne: BenkSøknaderKolonne = BenkSøknaderKolonne.KRAVTIDSPUNKT,
        retning: BenkV2SorteringRetning = BenkV2SorteringRetning.ASC,
    ) = command(BenkSøknaderFiltrering(status, søknadstype, saksbehandler), kolonne, retning)

    private fun revurderingerCommand(
        status: BenkV2Behandlingsstatus? = null,
        resultat: BenkRevurderingResultat? = null,
        saksbehandler: String? = null,
    ) = command(BenkRevurderingerFiltrering(status, resultat, saksbehandler), BenkRevurderingerKolonne.STARTET)

    private fun meldekortCommand(
        status: BenkV2Behandlingsstatus? = null,
        type: BenkMeldekortType? = null,
        saksbehandler: String? = null,
    ) = command(BenkMeldekortFiltrering(status, type, saksbehandler), BenkMeldekortKolonne.PERIODE)

    private fun klageCommand(
        status: BenkV2Behandlingsstatus? = null,
        resultat: BenkKlagebehandlingResultat? = null,
        saksbehandler: String? = null,
    ) = command(BenkKlageFiltrering(status, resultat, saksbehandler), BenkKlageKolonne.KRAVTIDSPUNKT)

    private fun tilbakekrevingCommand(
        status: BenkTilbakekrevingStatus? = null,
        kilde: BenkTilbakekrevingKilde? = null,
        saksbehandler: String? = null,
        minstebeløp: Long = 0,
    ) = command(
        BenkTilbakekrevingFiltrering(status, kilde, saksbehandler, minstebeløp),
        BenkTilbakekrevingKolonne.STARTET,
    )

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen viser både søknader uten behandling og åpne søknadsbehandlinger`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakUtenBehandling, søknad) = opprettSakOgSøknad(tac = tac)
            val (sakUnderBehandling, _, underBehandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac)
            // Iverksatte behandlinger er ferdige, og skal ikke ligge på benken.
            iverksettSøknadsbehandling(tac = tac)

            val oversikt = tac.benkV2Context.benkV2Repo.hentSøknader(søknaderCommand())

            oversikt.totalAntall shouldBe 2
            oversikt.totalAntallUfiltrert shouldBe 2
            oversikt.behandlinger.size shouldBe 2

            val utenBehandling = oversikt.behandlinger.single { it.felles.sakId == sakUtenBehandling.id }
            utenBehandling.felles.fnr shouldBe søknad.fnr
            utenBehandling.felles.saksnummer shouldBe søknad.saksnummer
            utenBehandling.felles.startet shouldBe søknad.opprettet
            utenBehandling.felles.sistEndret shouldBe søknad.opprettet
            utenBehandling.felles.saksbehandler shouldBe null
            utenBehandling.felles.beslutter shouldBe null
            utenBehandling.felles.erUnderkjent shouldBe false
            utenBehandling.felles.ventestatus.erSattPåVent shouldBe false
            utenBehandling.felles.ventestatus.begrunnelse shouldBe null
            utenBehandling.felles.ventestatus.frist shouldBe null
            utenBehandling.status shouldBe BenkV2Behandlingsstatus.KLAR_TIL_BEHANDLING
            utenBehandling.søknadstype shouldBe BenkSøknadstype.DIGITAL
            utenBehandling.kravtidspunkt shouldBe søknad.opprettet
            utenBehandling.resultat shouldBe null

            val medBehandling = oversikt.behandlinger.single { it.felles.sakId == sakUnderBehandling.id }
            medBehandling.status shouldBe BenkV2Behandlingsstatus.UNDER_BEHANDLING
            medBehandling.resultat shouldBe BenkSøknadsbehandlingResultat.INNVILGELSE
            medBehandling.felles.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
            medBehandling.felles.startet shouldBe underBehandling.opprettet
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen viser underkjent og ventestatus`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakUnderkjent) = underkjenn(tac = tac)
            val (sakPåVent) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!

            val oversikt = tac.benkV2Context.benkV2Repo.hentSøknader(søknaderCommand())

            oversikt.behandlinger.single { it.felles.sakId == sakUnderkjent.id }.felles.erUnderkjent shouldBe true
            oversikt.behandlinger.single { it.felles.sakId == sakPåVent.id }.felles.ventestatus.let {
                it.erSattPåVent shouldBe true
                (it.begrunnelse != null) shouldBe true
                (it.frist != null) shouldBe true
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen filtrerer på status, søknadstype og saksbehandler`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSakOgSøknad(tac = tac)
            val (sakUnderBehandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac)
            val repo = tac.benkV2Context.benkV2Repo

            repo.hentSøknader(søknaderCommand(status = BenkV2Behandlingsstatus.UNDER_BEHANDLING)).let {
                it.totalAntall shouldBe 1
                it.totalAntallUfiltrert shouldBe 2
                it.behandlinger.single().felles.sakId shouldBe sakUnderBehandling.id
            }
            repo.hentSøknader(søknaderCommand(søknadstype = BenkSøknadstype.DIGITAL)).totalAntall shouldBe 2
            repo.hentSøknader(søknaderCommand(søknadstype = BenkSøknadstype.PAPIR_SKJEMA)).let {
                it.totalAntall shouldBe 0
                it.totalAntallUfiltrert shouldBe 2
                it.behandlinger shouldBe emptyList()
            }
            repo.hentSøknader(søknaderCommand(saksbehandler = ObjectMother.saksbehandler().navIdent)).let {
                it.totalAntall shouldBe 1
                it.behandlinger.single().felles.sakId shouldBe sakUnderBehandling.id
            }
            repo.hentSøknader(søknaderCommand(saksbehandler = BenkV2Filtrering.IKKE_TILDELT)).totalAntall shouldBe 1
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen sorterer stigende og synkende, og respekterer limit`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            // Søknadene mottas med samme tidsstempel, så fnr er den eneste kolonnen sorteringen kan observeres på her.
            val fnrEn = Fnr.fromString("01019012345")
            val fnrTo = Fnr.fromString("02019012345")
            opprettSakOgSøknad(tac = tac, fnr = fnrTo)
            opprettSakOgSøknad(tac = tac, fnr = fnrEn)
            val repo = tac.benkV2Context.benkV2Repo

            fun kommando(retning: BenkV2SorteringRetning) =
                søknaderCommand(kolonne = BenkSøknaderKolonne.FNR, retning = retning)

            repo.hentSøknader(kommando(BenkV2SorteringRetning.ASC)).behandlinger.map { it.felles.fnr } shouldBe
                listOf(fnrEn, fnrTo)
            repo.hentSøknader(kommando(BenkV2SorteringRetning.DESC)).behandlinger.map { it.felles.fnr } shouldBe
                listOf(fnrTo, fnrEn)
            repo.hentSøknader(kommando(BenkV2SorteringRetning.ASC), limit = 1).let {
                // Limit kutter radene, men ikke tellingene: benken skal kunne si at den viser én av to.
                it.behandlinger.size shouldBe 1
                it.totalAntall shouldBe 2
            }
            repo.hentSøknader(søknaderCommand()).behandlinger.size shouldBe 2
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `revurderingsfanen viser åpne revurderinger`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac = tac)
            oppdaterRevurderingStans(tac = tac, sakId = sak.id, behandlingId = revurdering.id)
            val repo = tac.benkV2Context.benkV2Repo

            val oversikt = repo.hentRevurderinger(revurderingerCommand())

            oversikt.totalAntall shouldBe 1
            oversikt.behandlinger.single().let {
                it.felles.sakId shouldBe sak.id
                it.felles.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
                it.status shouldBe BenkV2Behandlingsstatus.UNDER_BEHANDLING
                it.resultat shouldBe BenkRevurderingResultat.STANS
            }

            repo.hentRevurderinger(revurderingerCommand(resultat = BenkRevurderingResultat.STANS)).totalAntall shouldBe 1
            repo.hentRevurderinger(
                revurderingerCommand(resultat = BenkRevurderingResultat.OMGJØRING),
            ).totalAntall shouldBe 0
            repo.hentRevurderinger(
                revurderingerCommand(status = BenkV2Behandlingsstatus.KLAR_TIL_BESLUTNING),
            ).totalAntall shouldBe 0
            repo.hentRevurderinger(
                revurderingerCommand(saksbehandler = BenkV2Filtrering.IKKE_TILDELT),
            ).totalAntall shouldBe 0
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `meldekortfanen viser meldekortbehandlinger med beregnet beløp og periode`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakUtenBeregning, _, _, meldekortbehandling) =
                iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!
            val (sakMedBeregning) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac = tac)!!
            val repo = tac.benkV2Context.benkV2Repo

            val oversikt = repo.hentMeldekort(meldekortCommand())

            oversikt.totalAntall shouldBe 2
            oversikt.behandlinger.single { it.felles.sakId == sakUtenBeregning.id }.let {
                it.type shouldBe BenkMeldekortType.MELDEKORTBEHANDLING
                it.status shouldBe BenkV2Behandlingsstatus.UNDER_BEHANDLING
                it.periode shouldBe meldekortbehandling.periode
                it.beløp shouldBe null
                it.mottattTidspunkt shouldBe null
                it.felles.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
            }
            oversikt.behandlinger.single { it.felles.sakId == sakMedBeregning.id }.let {
                it.status shouldBe BenkV2Behandlingsstatus.KLAR_TIL_BESLUTNING
                (it.beløp!! > 0) shouldBe true
            }

            repo.hentMeldekort(meldekortCommand(type = BenkMeldekortType.MELDEKORTBEHANDLING)).totalAntall shouldBe 2
            repo.hentMeldekort(meldekortCommand(type = BenkMeldekortType.INNSENDT_MELDEKORT)).totalAntall shouldBe 0
            repo.hentMeldekort(
                meldekortCommand(status = BenkV2Behandlingsstatus.KLAR_TIL_BESLUTNING),
            ).totalAntall shouldBe 1
            repo.hentMeldekort(meldekortCommand(saksbehandler = BenkV2Filtrering.IKKE_TILDELT)).totalAntall shouldBe 0
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `meldekortfanen viser innsendte og korrigerte meldekort som venter på behandling`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            // Første kort i kjeden behandles automatisk; korrigeringen etterpå faller til manuell behandling og havner på benken.
            mottaAutomatiskMeldekortForKjede(tac, sak, kjedeIndeks = 0)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()
            tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()
            val (_, korrigering) = mottaManueltMeldekortForKjede(tac, sak, kjedeIndeks = 0)
            val (_, innsendt) = mottaManueltMeldekortForKjede(tac, sak, kjedeIndeks = 1)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            val repo = tac.benkV2Context.benkV2Repo

            val oversikt = repo.hentMeldekort(meldekortCommand())

            oversikt.behandlinger.map { it.type } shouldBe listOf(
                BenkMeldekortType.KORRIGERT_MELDEKORT,
                BenkMeldekortType.INNSENDT_MELDEKORT,
            )
            oversikt.behandlinger.first().let {
                it.felles.startet shouldBe korrigering.mottatt
                it.mottattTidspunkt shouldBe korrigering.mottatt
                it.beløp shouldBe null
                it.status shouldBe BenkV2Behandlingsstatus.KLAR_TIL_BEHANDLING
                it.felles.saksbehandler shouldBe null
            }
            oversikt.behandlinger.last().mottattTidspunkt shouldBe innsendt.mottatt

            repo.hentMeldekort(meldekortCommand(type = BenkMeldekortType.KORRIGERT_MELDEKORT)).totalAntall shouldBe 1
            repo.hentMeldekort(meldekortCommand(saksbehandler = BenkV2Filtrering.IKKE_TILDELT)).totalAntall shouldBe 2
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `klagefanen viser åpne klagebehandlinger`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sakAvvist, klageAvvist) = opprettSakOgKlagebehandlingTilAvvisning(tac = tac)!!
            val (sakMedResultat, _, klageMedResultat) = opprettSakOgKlagebehandlingTilOpprettholdelse(tac = tac)!!
            val repo = tac.benkV2Context.benkV2Repo

            val oversikt = repo.hentKlager(klageCommand())

            oversikt.totalAntall shouldBe 2
            oversikt.behandlinger.single { it.felles.sakId == sakAvvist.id }.let {
                it.felles.saksbehandler shouldBe saksbehandler.navIdent
                it.felles.beslutter shouldBe null
                it.felles.erUnderkjent shouldBe false
                it.status shouldBe BenkV2Behandlingsstatus.UNDER_BEHANDLING
                it.resultat shouldBe BenkKlagebehandlingResultat.AVVIST
                it.felles.startet shouldBe klageAvvist.opprettet
            }
            oversikt.behandlinger.single { it.felles.sakId == sakMedResultat.id }.let {
                it.resultat shouldBe BenkKlagebehandlingResultat.OPPRETTHOLDT
                it.felles.sistEndret shouldBe klageMedResultat.sistEndret
            }

            repo.hentKlager(klageCommand(resultat = BenkKlagebehandlingResultat.OPPRETTHOLDT)).totalAntall shouldBe 1
            repo.hentKlager(klageCommand(resultat = BenkKlagebehandlingResultat.OMGJØR)).totalAntall shouldBe 0
            repo.hentKlager(klageCommand(status = BenkV2Behandlingsstatus.UNDER_BEHANDLING)).totalAntall shouldBe 2
            repo.hentKlager(klageCommand(saksbehandler = saksbehandler.navIdent)).totalAntall shouldBe 2
            repo.hentKlager(klageCommand(saksbehandler = BenkV2Filtrering.IKKE_TILDELT)).totalAntall shouldBe 0
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `tilbakekrevingsfanen viser åpne tilbakekrevinger med utledet status`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakTilBehandling, tilBehandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val (sakTilGodkjenning) = opprettTilbakekrevingBehandlingTilGodkjenning(tac = tac)
            val repo = tac.benkV2Context.benkV2Repo

            val oversikt = repo.hentTilbakekrevinger(tilbakekrevingCommand())

            oversikt.totalAntall shouldBe 2
            oversikt.behandlinger.single { it.felles.sakId == sakTilBehandling.id }.let {
                it.status shouldBe BenkTilbakekrevingStatus.TIL_BEHANDLING
                it.kilde shouldBe BenkTilbakekrevingKilde.MELDEKORT
                it.beløp shouldBe tilBehandling.totaltFeilutbetaltBeløp
                it.kravgrunnlagPeriode shouldBe tilBehandling.kravgrunnlagTotalPeriode
                it.felles.saksbehandler shouldBe null
                it.felles.beslutter shouldBe null
                it.felles.erUnderkjent shouldBe false
                it.felles.ventestatus.erSattPåVent shouldBe false
            }
            oversikt.behandlinger.single { it.felles.sakId == sakTilGodkjenning.id }.status shouldBe
                BenkTilbakekrevingStatus.TIL_GODKJENNING

            repo.hentTilbakekrevinger(
                tilbakekrevingCommand(status = BenkTilbakekrevingStatus.TIL_BEHANDLING),
            ).totalAntall shouldBe 1
            repo.hentTilbakekrevinger(
                tilbakekrevingCommand(kilde = BenkTilbakekrevingKilde.MELDEKORT),
            ).totalAntall shouldBe 2
            repo.hentTilbakekrevinger(
                tilbakekrevingCommand(kilde = BenkTilbakekrevingKilde.RAMMEVEDTAK),
            ).totalAntall shouldBe 0
            repo.hentTilbakekrevinger(
                tilbakekrevingCommand(saksbehandler = BenkV2Filtrering.IKKE_TILDELT),
            ).totalAntall shouldBe 2
            // Minstebeløpsfilteret er benkens eneste tallfilter, og skal kutte begge veier.
            repo.hentTilbakekrevinger(tilbakekrevingCommand(minstebeløp = 1)).totalAntall shouldBe 2
            repo.hentTilbakekrevinger(tilbakekrevingCommand(minstebeløp = 1_000_000)).totalAntall shouldBe 0
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `antall per fane telles uten filter`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSakOgSøknad(tac = tac)
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac = tac)
            oppdaterRevurderingStans(tac = tac, sakId = sak.id, behandlingId = revurdering.id)
            iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)
            opprettSakOgKlagebehandlingTilAvvisning(tac = tac)
            opprettTilbakekrevingBehandlingTilBehandling(tac = tac)

            tac.benkV2Context.benkV2Repo.hentAntallPerFane() shouldBe
                no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2AntallPerFane(
                    søknader = 1,
                    revurderinger = 1,
                    meldekort = 1,
                    klage = 1,
                    tilbakekreving = 1,
                )
        }
    }

    /** Sender inn et rent brukers meldekort via motta-ruta, klart for automatisk behandling. */
    private suspend fun ApplicationTestBuilder.mottaAutomatiskMeldekortForKjede(
        tac: TestApplicationContextMedPostgres,
        sak: Sak,
        kjedeIndeks: Int = 0,
    ): Pair<Sak, BrukersMeldekort> {
        val meldeperiode = sak.meldeperiodeKjeder[kjedeIndeks].hentSisteMeldeperiode()
        val (oppdatertSak, brukersMeldekort, _) = mottaMeldekortRequest(
            tac = tac,
            meldeperiodeId = meldeperiode.id,
            sakId = sak.id,
            dager = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort),
            journalpostId = UUID.randomUUID().toString(),
        )
        return oppdatertSak to brukersMeldekort!!
    }

    /**
     * Sender inn brukers meldekort med for mye sammenhengende godkjent fravær via motta-ruta.
     * Ruta setter `behandlesAutomatisk = true` på alle kort, så prodstien til benken går gjennom den automatiske jobben: den gir opp kortet og markerer det for manuell behandling.
     */
    private suspend fun ApplicationTestBuilder.mottaManueltMeldekortForKjede(
        tac: TestApplicationContextMedPostgres,
        sak: Sak,
        kjedeIndeks: Int = 0,
    ): Pair<Sak, BrukersMeldekort> {
        val meldeperiode = sak.meldeperiodeKjeder[kjedeIndeks].hentSisteMeldeperiode()
        val utfylt = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort)
        val fraværsdager = utfylt.keys
            .filter { utfylt.getValue(it) != BrukerutfyltMeldekortDTO.Status.IKKE_BESVART }
            .sorted()
            .windowed(MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER + 1)
            .first { vindu -> vindu.zipWithNext().all { (a, b) -> b == a.plusDays(1) } }
            .toSet()
        val dagerMedFravær = utfylt.mapValues { (dato, status) ->
            if (dato in fraværsdager) BrukerutfyltMeldekortDTO.Status.FRAVÆR_GODKJENT_AV_NAV else status
        }
        val (oppdatertSak, brukersMeldekort, _) = mottaMeldekortRequest(
            tac = tac,
            meldeperiodeId = meldeperiode.id,
            sakId = sak.id,
            dager = dagerMedFravær,
            journalpostId = UUID.randomUUID().toString(),
        )
        return oppdatertSak to brukersMeldekort!!
    }
}
