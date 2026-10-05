package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.angre.angreMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgAvbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggTilbakeMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settMeldekortbehandlingPåVent
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

    @Test
    fun `angreMeldekortbehandling gir sann når saksbehandler angrer meldekortbehandlingen sendt til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->

            val saksbehandler = ObjectMother.saksbehandler()

            val (_, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac, saksbehandler = saksbehandler)!!

            val angretMeldekortbehandling = meldekortbehandling.angreMeldekortbehandling(saksbehandler, ObjectMother.clock).getOrFail()

            tac.meldekortContext.meldekortbehandlingRepo.angreBehandling(meldekortbehandling = angretMeldekortbehandling, transactionContext = null) shouldBe true

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
                it.sendtTilBeslutning shouldBe null
            }
        }
    }

    @Test
    fun `angreBehandling avviser en utdatert angring etter at beslutter har satt meldekortbehandlingen på vent`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandler")
            val beslutter = ObjectMother.beslutter("beslutter")
            val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(
                tac = tac,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
            )!!
            val angretBehandling = meldekortbehandling.angreMeldekortbehandling(saksbehandler, tac.clock).getOrFail()
            val (_, behandlingPåVent) = settMeldekortbehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = beslutter,
            )!!
            val repo = tac.meldekortContext.meldekortbehandlingRepo

            repo.angreBehandling(angretBehandling, null) shouldBe false
            repo.hent(meldekortbehandling.id) shouldBe behandlingPåVent
        }
    }

    @Test
    fun `angreMeldekortbehandling gir sann når saksbehandler angrer en meldekortbehandling sendt til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(tac)!!

            tac.meldekortContext.meldekortbehandlingRepo.angreBehandling(meldekortbehandling = meldekortbehandling, transactionContext = null) shouldBe true
        }
    }

    @Test
    fun `angreMeldekortbehandling gir false når meldekortbehandlingen ikke er sendt til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac)!!

            tac.meldekortContext.meldekortbehandlingRepo.angreBehandling(meldekortbehandling = meldekortbehandling, transactionContext = null) shouldBe false

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
        }
    }

    /**
     * En avbrutt meldekortbehandling kan aldri være under beslutning, så vakten slår ikke til.
     * Testen øver `avbrutt != null`-grenen i parameterlisten.
     */
    @Test
    fun `oppdaterHvisFortsattUnderBeslutning gir false for en avbrutt meldekortbehandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, avbruttMeldekortbehandling, _) = iverksettSøknadsbehandlingOgAvbrytMeldekortbehandling(tac)!!
            avbruttMeldekortbehandling.avbrutt.shouldNotBeNull()

            tac.meldekortContext.meldekortbehandlingRepo.oppdaterHvisFortsattUnderBeslutning(
                meldekortbehandling = avbruttMeldekortbehandling,
                utøvendeBeslutter = ObjectMother.beslutter(),
                transactionContext = null,
            ) shouldBe false

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = avbruttMeldekortbehandling.id)!!.status shouldBe MeldekortbehandlingStatus.AVBRUTT
        }
    }

    @Test
    fun `oppdaterHvisFortsattUnderBeslutning gir sann når meldekortbehandlingen fortsatt er under beslutning hos den samme beslutteren`() {
        withTestApplicationContextAndPostgres { tac ->
            val beslutter = ObjectMother.beslutter("beslutter")
            val (_, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(tac, beslutter = beslutter)!!
            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING

            tac.meldekortContext.meldekortbehandlingRepo.oppdaterHvisFortsattUnderBeslutning(
                meldekortbehandling = meldekortbehandling,
                utøvendeBeslutter = beslutter,
                transactionContext = null,
            ) shouldBe true

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING
                it.beslutter shouldBe beslutter.navIdent
            }
        }
    }

    /**
     * Vakten `beslutter = :forventet_beslutter` slår ikke til når en annen beslutter har overtatt behandlingen i mellomtiden.
     */
    @Test
    fun `oppdaterHvisFortsattUnderBeslutning gir false når en annen beslutter eier meldekortbehandlingen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(
                tac,
                beslutter = ObjectMother.beslutter("beslutterSomVant"),
            )!!

            tac.meldekortContext.meldekortbehandlingRepo.oppdaterHvisFortsattUnderBeslutning(
                meldekortbehandling = meldekortbehandling,
                utøvendeBeslutter = ObjectMother.beslutter("beslutterSomAldriEide"),
                transactionContext = null,
            ) shouldBe false

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.beslutter shouldBe "beslutterSomVant"
        }
    }

    /**
     * Meldekortbehandlingen er klar til beslutning og ikke tatt av noen beslutter, så vakten `status = 'UNDER_BESLUTNING'` slår ikke til.
     */
    @Test
    fun `oppdaterHvisFortsattUnderBeslutning gir false når meldekortbehandlingen ikke er under beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac)!!

            tac.meldekortContext.meldekortbehandlingRepo.oppdaterHvisFortsattUnderBeslutning(
                meldekortbehandling = meldekortbehandling,
                utøvendeBeslutter = ObjectMother.beslutter(),
                transactionContext = null,
            ) shouldBe false

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
        }
    }

    /**
     * En meldekortbehandling som er opprettet fra en klage bærer klagebehandlingen med seg.
     * Klagebehandlingen skal bare lagres når vakten slapp gjennom, så et tapt kappløp skal ikke berøre den.
     */
    @Test
    fun `oppdaterHvisFortsattUnderBeslutning gir false når den klagekoblede meldekortbehandlingen ikke er under beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, tattMeldekortbehandling, _) = tattMeldekortbehandlingMedKlageFraKlageRoute(tac = tac)!!
            tattMeldekortbehandling.klagebehandling.shouldNotBeNull()

            tac.meldekortContext.meldekortbehandlingRepo.oppdaterHvisFortsattUnderBeslutning(
                meldekortbehandling = tattMeldekortbehandling,
                utøvendeBeslutter = ObjectMother.beslutter(),
                transactionContext = null,
            ) shouldBe false
        }
    }

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
