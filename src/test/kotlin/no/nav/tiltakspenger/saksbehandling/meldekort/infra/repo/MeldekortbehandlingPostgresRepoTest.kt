package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggTilbakeMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tattMeldekortbehandlingMedKlageFraKlageRoute
import org.junit.jupiter.api.Test

/**
 * Tildelingsmetodene i [MeldekortbehandlingPostgresRepo] har en `where`-vakt på eierskap, og fungerer som en optimistisk lås.
 * Taper vi kappløpet mot en annen saksbehandler, treffer oppdateringen ingen rader og metoden returnerer `false`.
 *
 * Servicelaget vurderer tildelingen på en fersk lesning av behandlingen, så gjennom rutene er vakten alltid sann.
 * Det tapte kappløpet kan derfor bare øves ved å kalle repoet direkte.
 * Tilstanden bygges gjennom prodstien, og bare selve kallet går utenom.
 *
 * Sann-siden av de samme vaktene er dekket av rutetestene i `meldekort/infra/route/`.
 */
class MeldekortbehandlingPostgresRepoTest {

    /**
     * Rutetestene for tildeling (`TaMeldekortbehandlingRouteTest` med flere) kjører mot fakes, ikke postgres.
     * Den klageløse tildelingsstien har derfor aldri kjørt mot ekte SQL — bare de klagekoblede testene gjør det.
     * Denne testen kjører hele runden gjennom prodstien mot databasen, slik at også `klagebehandling == null`-grenen er øvd.
     */
    @Test
    fun `tildeling av en meldekortbehandling uten koblet klagebehandling går gjennom databasen`() {
        withTestApplicationContextAndPostgres { tac ->
            // Behandlingen er allerede tatt av `saksbehandlerSomOppretter`, så runden starter på overta.
            val saksbehandlerSomOppretter = ObjectMother.saksbehandlerOgBeslutter("saksbehandlerSomOppretter")
            val saksbehandlerSomOvertar = ObjectMother.saksbehandlerOgBeslutter("saksbehandlerSomOvertar")
            val saksbehandlerSomTar = ObjectMother.saksbehandlerOgBeslutter("saksbehandlerSomTar")
            val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
                tac = tac,
                saksbehandler = saksbehandlerSomOppretter,
            )!!

            overtaMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                overtarFraSaksbehandlerEllerBeslutter = saksbehandlerSomOppretter,
                saksbehandlerEllerBeslutterSomOvertar = saksbehandlerSomOvertar,
            )!!.second.saksbehandler shouldBe saksbehandlerSomOvertar.navIdent

            leggTilbakeMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = saksbehandlerSomOvertar,
            )!!.second.saksbehandler shouldBe null

            taMeldekortbehanding(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = saksbehandlerSomTar,
            )!!.second.saksbehandler shouldBe saksbehandlerSomTar.navIdent
        }
    }

    /**
     * En meldekortbehandling som er opprettet fra en klage bærer klagebehandlingen med seg.
     * Da oppdaterer alle de tre saksbehandlermetodene klagebehandlingen i samme transaksjon, før den optimistiske låsen slår til.
     * Den koblede grenen kjører altså uansett utfall, og testen dekker både den og det tapte kappløpet.
     */
    @Test
    fun `saksbehandler som taper kappløpet får false fra alle tildelingsmetodene`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, tattMeldekortbehandling, _) = tattMeldekortbehandlingMedKlageFraKlageRoute(tac = tac)!!
            tattMeldekortbehandling.klagebehandling.shouldNotBeNull()
            val repo = tac.meldekortContext.meldekortbehandlingRepo

            // Behandlingen har allerede en saksbehandler, så `saksbehandler is null` slår ikke til.
            repo.taBehandlingSaksbehandler(tattMeldekortbehandling) shouldBe false

            // Vi tror en annen saksbehandler eier behandlingen enn den som faktisk står der.
            repo.overtaSaksbehandler(tattMeldekortbehandling, "saksbehandlerSomAldriEide") shouldBe false
            repo.leggTilbakeBehandlingSaksbehandler(
                tattMeldekortbehandling,
                ObjectMother.saksbehandler("saksbehandlerSomAldriEide"),
            ) shouldBe false
        }
    }

    /**
     * Beslutterrollen har id-baserte signaturer og bærer derfor ingen klagebehandling.
     * Oppsettet fram til en tatt beslutning er dyrt, så alle tre metodene øves på den samme behandlingen.
     */
    @Test
    fun `beslutter som taper kappløpet får false fra alle tildelingsmetodene`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, tattMeldekortbehandling, _) =
                iverksettSøknadsbehandlingOgBeslutterTarBehandling(tac = tac)!!
            val repo = tac.meldekortContext.meldekortbehandlingRepo
            val meldekortId = tattMeldekortbehandling.id
            val sistEndret = tattMeldekortbehandling.sistEndret
            val status = tattMeldekortbehandling.status

            // Behandlingen har allerede en beslutter, så `beslutter is null` slår ikke til.
            repo.taBehandlingBeslutter(
                meldekortId,
                ObjectMother.beslutter("beslutterSomAldriEide"),
                status,
                sistEndret,
            ) shouldBe false

            // Vi tror en annen beslutter eier behandlingen enn den som faktisk står der.
            repo.overtaBeslutter(
                meldekortId,
                ObjectMother.beslutter("nyBeslutter"),
                "beslutterSomAldriEide",
                sistEndret,
            ) shouldBe false
            repo.leggTilbakeBehandlingBeslutter(
                meldekortId,
                ObjectMother.beslutter("beslutterSomAldriEide"),
                status,
                sistEndret,
            ) shouldBe false
        }
    }
}
