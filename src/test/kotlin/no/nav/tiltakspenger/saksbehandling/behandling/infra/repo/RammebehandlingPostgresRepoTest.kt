package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.angre.angreBehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.angreRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgTaKlagebehandlingMedRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjennForBehandlingId
import org.junit.jupiter.api.Test

/**
 * Tildelingsmetodene i [RammebehandlingPostgresRepo] har en `where`-vakt på både eierskap og status, og fungerer som en optimistisk lås.
 * Taper vi kappløpet mot en annen saksbehandler, treffer oppdateringen ingen rader og metoden returnerer `false`.
 *
 * Servicelaget vurderer tildelingen på en fersk lesning av behandlingen, så gjennom rutene er vakten alltid sann.
 * Det tapte kappløpet kan derfor bare øves ved å kalle repoet direkte.
 * Tilstanden bygges gjennom prodstien, og bare selve kallet går utenom.
 *
 * Sann-siden av de samme vaktene er dekket av rutetestene i `behandling/infra/route/taOgOverta/`.
 */
class RammebehandlingPostgresRepoTest {

    /**
     * Behandlingen er allerede tatt, så `saksbehandler is null and status = 'KLAR_TIL_BEHANDLING'` slår ikke til.
     */
    @Test
    fun `taBehandlingSaksbehandler gir false når behandlingen alt er tatt`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac = tac)

            tac.behandlingContext.rammebehandlingRepo.taBehandlingSaksbehandler(behandling, null) shouldBe false
        }
    }

    /**
     * `TaOgOvertaRammebehandlingTest` kjører saksbehandlervarianten av overta in-memory.
     * Den klageløse grenen i `overtaSaksbehandler` har derfor aldri kjørt mot ekte SQL — bare de klagekoblede testene gjør det.
     * Denne testen tar runden gjennom prodstien mot databasen.
     */
    @Test
    fun `overtaSaksbehandler på en behandling uten klagebehandling går gjennom databasen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(
                tac = tac,
                saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomHadde"),
            )
            // Overta er sperret i en time etter siste aktivitet på behandlingen.
            tac.clock.spol1timeFrem()

            val (_, overtattBehandling, _) = overtaBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                overtarFra = "saksbehandlerSomHadde",
                saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertar"),
            )!!

            overtattBehandling.saksbehandler shouldBe "saksbehandlerSomOvertar"
        }
    }

    /**
     * Beslutterrollen har sin egen vakt, og oppsettet fram til `KLAR_TIL_BESLUTNING` er dyrt.
     * Derfor øves både `ta` og `overta` på den samme behandlingen.
     */
    @Test
    fun `beslutter som taper kappløpet får false fra ta og overta`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandlingId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            taRammebehandlinger(
                tac = tac,
                behandlinger = listOf(sak.id to behandlingId),
                saksbehandler = ObjectMother.beslutter("beslutterSomVant"),
            )!!
            val repo = tac.behandlingContext.rammebehandlingRepo
            val tattBehandling = repo.hent(behandlingId)

            // Behandlingen har allerede en beslutter, så `beslutter is null` slår ikke til.
            repo.taBehandlingBeslutter(tattBehandling, null) shouldBe false

            // Vi tror en annen beslutter eier behandlingen enn den som faktisk står der.
            repo.overtaBeslutter(tattBehandling, "beslutterSomAldriEide", null) shouldBe false
        }
    }

    /**
     * En rammebehandling som er opprettet fra en klage bærer klagebehandlingen med seg.
     * Da oppdaterer `overtaSaksbehandler` klagebehandlingen i samme transaksjon, før den optimistiske låsen slår til.
     * Denne testen dekker derfor både den koblede grenen og det tapte kappløpet.
     */
    @Test
    fun `overtaSaksbehandler gir false når en annen saksbehandler eier den klagekoblede behandlingen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, rammebehandlingMedKlagebehandling, _) =
                iverksettSøknadsbehandlingOgTaKlagebehandlingMedRammebehandling(tac = tac)!!
            rammebehandlingMedKlagebehandling.klagebehandling.shouldNotBeNull()

            tac.behandlingContext.rammebehandlingRepo.overtaSaksbehandler(
                rammebehandlingMedKlagebehandling,
                "saksbehandlerSomAldriEide",
                null,
            ) shouldBe false
        }
    }

    @Test
    fun `angreBehandling gir sann når saksbehandler angrer en behandling klar til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val (_, _, behandlingId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac, saksbehandler = saksbehandler)

            val behandlingFørAngring = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            val (angretBehandling, _) = behandlingFørAngring.angreBehandling(saksbehandler, ObjectMother.clock).getOrFail()

            tac.behandlingContext.rammebehandlingRepo.angreBehandling(
                angretBehandling,
                behandlingFørAngring.sendtTilBeslutning,
                null,
            ) shouldBe true

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.sendtTilBeslutning shouldBe null
            }
        }
    }

    /**
     * Saksbehandleren har lastet behandlingen før beslutteren underkjente, og den er sendt inn på nytt før angringen når databasen.
     * Underkjenning beholder beslutteren, så den nye innsendingen går rett til `UNDER_BESLUTNING`.
     * Status, saksbehandler og beslutter er de samme som da behandlingen ble lastet, så bare `sendt_til_beslutning` skiller de to innsendingene.
     */
    @Test
    fun `angreBehandling avviser en utdatert angring etter at behandlingen er sendt til beslutning på nytt`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val beslutter = ObjectMother.beslutter()
            val (sak, _, behandlingId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac, saksbehandler = saksbehandler)
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), beslutter)!!
            val utdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            val (angretBehandling, _) = utdatertBehandling.angreBehandling(saksbehandler, tac.clock).getOrFail()

            underkjennForBehandlingId(tac, sak.id, behandlingId, beslutter = beslutter)
            sendSøknadsbehandlingTilBeslutningForBehandlingId(tac, sak.id, behandlingId, saksbehandler = saksbehandler)
            val nyInnsending = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            nyInnsending.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
            nyInnsending.beslutter shouldBe beslutter.navIdent

            tac.behandlingContext.rammebehandlingRepo.angreBehandling(
                angretBehandling,
                utdatertBehandling.sendtTilBeslutning,
                null,
            ) shouldBe false

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId) shouldBe nyInnsending
        }
    }

    /**
     * Beslutteren har lastet behandlingen, og saksbehandleren har angret og sendt inn på nytt.
     * Beslutteren beholdes ved angring, så den nye innsendingen går rett til `UNDER_BESLUTNING` hos den samme beslutteren.
     * Status og beslutter er de samme som da behandlingen ble lastet, så bare `sendt_til_beslutning` skiller de to innsendingene.
     */
    @Test
    fun `lagreHvisFortsattUnderBeslutning avviser en utdatert skriving etter at behandlingen er sendt til beslutning på nytt`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val beslutter = ObjectMother.beslutter()
            val (sak, _, behandlingId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac, saksbehandler = saksbehandler)
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), beslutter)!!
            val utdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)

            angreRammebehandling(tac, sak.id, behandlingId, saksbehandler = saksbehandler)!!
            sendSøknadsbehandlingTilBeslutningForBehandlingId(tac, sak.id, behandlingId, saksbehandler = saksbehandler)
            val nyInnsending = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            nyInnsending.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
            nyInnsending.beslutter shouldBe beslutter.navIdent

            tac.behandlingContext.rammebehandlingRepo.lagreHvisFortsattUnderBeslutning(
                rammebehandling = utdatertBehandling,
                utøvendeBeslutter = beslutter,
                transactionContext = null,
            ) shouldBe false

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId) shouldBe nyInnsending
        }
    }

    @Test
    fun `angreBehandling avviser en utdatert angring etter at beslutter har satt behandlingen på vent`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val beslutter = ObjectMother.beslutter()
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac, saksbehandler = saksbehandler)
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), beslutter)!!
            val repo = tac.behandlingContext.rammebehandlingRepo
            val behandlingFørAngring = repo.hent(behandlingId)
            val (angretBehandling) = behandlingFørAngring.angreBehandling(saksbehandler, tac.clock).getOrFail()

            val (_, _, behandlingPåVent) = settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = beslutter,
            )!!

            repo.angreBehandling(angretBehandling, behandlingFørAngring.sendtTilBeslutning, null) shouldBe false
            repo.hent(behandlingId) shouldBe behandlingPåVent
        }
    }

    @Test
    fun `angreBehandling gir sann når saksbehandler angrer en behandling tatt av en beslutter`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val (sak, _, behandlingId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac, saksbehandler = saksbehandler)
            taRammebehandlinger(
                tac = tac,
                behandlinger = listOf(sak.id to behandlingId),
                saksbehandler = ObjectMother.beslutter(),
            )!!
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING

            val behandlingFørAngring = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            val (angretBehandling, _) = behandlingFørAngring.angreBehandling(saksbehandler, ObjectMother.clock).getOrFail()

            tac.behandlingContext.rammebehandlingRepo.angreBehandling(angretBehandling, behandlingFørAngring.sendtTilBeslutning, null) shouldBe true

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.beslutter shouldBe "B12345"
                it.sendtTilBeslutning shouldBe null
            }
        }
    }

    /**
     * `angreBehandling` har en `where`-vakt på både eierskap og status: `saksbehandler = :saksbehandler and status in ('KLAR_TIL_BESLUTNING', 'UNDER_BESLUTNING')`.
     * Behandlingen er under behandling, så vakten slår ikke til.
     * Sann-siden er dekket av testene over og av `AngreRammebehandlingRouteTest`.
     */
    @Test
    fun `angreBehandling gir false når behandlingen ikke er klar til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac = tac)

            tac.behandlingContext.rammebehandlingRepo.angreBehandling(behandling, behandling.sendtTilBeslutning, null) shouldBe false
        }
    }

    /**
     * En rammebehandling som er opprettet fra en klage bærer klagebehandlingen med seg.
     * Da berører `angreBehandling` klagebehandlingen i samme transaksjon, før den optimistiske låsen slår til.
     * Denne testen dekker derfor både den koblede grenen og det tapte kappløpet.
     */
    @Test
    fun `angreBehandling gir false når den klagekoblede behandlingen ikke er klar til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, rammebehandlingMedKlagebehandling, _) =
                iverksettSøknadsbehandlingOgTaKlagebehandlingMedRammebehandling(tac = tac)!!
            rammebehandlingMedKlagebehandling.klagebehandling.shouldNotBeNull()

            tac.behandlingContext.rammebehandlingRepo.angreBehandling(
                rammebehandlingMedKlagebehandling,
                rammebehandlingMedKlagebehandling.sendtTilBeslutning,
                null,
            ) shouldBe false
        }
    }

    @Test
    fun `lagreHvisFortsattUnderBeslutning gir sann når behandlingen fortsatt er under beslutning hos den samme beslutteren`() {
        withTestApplicationContextAndPostgres { tac ->
            val beslutter = ObjectMother.beslutter()
            val (sak, _, behandlingId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            taRammebehandlinger(
                tac = tac,
                behandlinger = listOf(sak.id to behandlingId),
                saksbehandler = beslutter,
            )!!
            val behandlingUnderBeslutning = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            behandlingUnderBeslutning.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING

            tac.behandlingContext.rammebehandlingRepo.lagreHvisFortsattUnderBeslutning(
                rammebehandling = behandlingUnderBeslutning,
                utøvendeBeslutter = beslutter,
                transactionContext = null,
            ) shouldBe true

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                it.beslutter shouldBe beslutter.navIdent
            }
        }
    }

    /**
     * Vakten `beslutter = :forventet_beslutter` slår ikke til når en annen beslutter har overtatt behandlingen i mellomtiden.
     */
    @Test
    fun `lagreHvisFortsattUnderBeslutning gir false når en annen beslutter eier behandlingen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandlingId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            taRammebehandlinger(
                tac = tac,
                behandlinger = listOf(sak.id to behandlingId),
                saksbehandler = ObjectMother.beslutter("beslutterSomVant"),
            )!!

            tac.behandlingContext.rammebehandlingRepo.lagreHvisFortsattUnderBeslutning(
                rammebehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId),
                utøvendeBeslutter = ObjectMother.beslutter("beslutterSomAldriEide"),
                transactionContext = null,
            ) shouldBe false

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).beslutter shouldBe "beslutterSomVant"
        }
    }

    /**
     * Behandlingen er under behandling, slik den er etter at saksbehandleren har angret, så vakten `status = 'UNDER_BESLUTNING'` slår ikke til.
     */
    @Test
    fun `lagreHvisFortsattUnderBeslutning gir false når behandlingen ikke lenger er under beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac = tac)

            tac.behandlingContext.rammebehandlingRepo.lagreHvisFortsattUnderBeslutning(
                rammebehandling = behandling,
                utøvendeBeslutter = ObjectMother.beslutter(),
                transactionContext = null,
            ) shouldBe false

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
        }
    }

    /**
     * En rammebehandling som er opprettet fra en klage bærer klagebehandlingen med seg.
     * Klagebehandlingen skal bare lagres når vakten slapp gjennom, så et tapt kappløp skal ikke berøre den.
     */
    @Test
    fun `lagreHvisFortsattUnderBeslutning gir false og lar klagebehandlingen være når den klagekoblede behandlingen ikke er under beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, rammebehandlingMedKlagebehandling, _) =
                iverksettSøknadsbehandlingOgTaKlagebehandlingMedRammebehandling(tac = tac)!!
            rammebehandlingMedKlagebehandling.klagebehandling.shouldNotBeNull()

            tac.behandlingContext.rammebehandlingRepo.lagreHvisFortsattUnderBeslutning(
                rammebehandling = rammebehandlingMedKlagebehandling,
                utøvendeBeslutter = ObjectMother.beslutter(),
                transactionContext = null,
            ) shouldBe false
        }
    }
}
