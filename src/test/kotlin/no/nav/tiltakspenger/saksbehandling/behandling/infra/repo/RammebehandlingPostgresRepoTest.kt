package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgTaKlagebehandlingMedRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaBehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
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

            val (_, overtattBehandling, _) = overtaBehanding(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                overtarFra = "saksbehandlerSomHadde",
                saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertar"),
            )

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
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
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
}
