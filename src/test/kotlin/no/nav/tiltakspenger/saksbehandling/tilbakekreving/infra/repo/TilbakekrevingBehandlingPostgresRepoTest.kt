package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilGodkjenning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tildelTilbakekrevingBehandling
import org.junit.jupiter.api.Test

/**
 * Tildelingsmetodene i [TilbakekrevingBehandlingPostgresRepo] har en `where`-vakt som gjør dem til en optimistisk lås.
 * Taper vi kappløpet mot en annen saksbehandler, treffer oppdateringen ingen rader og metoden returnerer `false`.
 *
 * Servicelaget vurderer tildelingen på en fersk lesning av behandlingen, så gjennom rutene er vakten alltid sann.
 * Det tapte kappløpet kan derfor bare øves ved å kalle repoet direkte.
 * Tilstanden bygges gjennom prodstien, og bare selve kallet går utenom.
 *
 * Sann-siden av de samme vaktene er dekket av rutetestene i `tilbakekreving/infra/route/`.
 */
class TilbakekrevingBehandlingPostgresRepoTest {

    /**
     * Oppsettet er dyrt (to kafka-hendelser og to jobbkjøringer per behandling), så alle tre saksbehandlerrollene øves på samme behandling.
     */
    @Test
    fun `saksbehandler som taper kappløpet får false fra alle tildelingsmetodene`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val (_, tattBehandling) = tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomVant"),
            )!!
            val repo = tac.tilbakekrevingBehandlingRepo

            // Behandlingen har allerede en saksbehandler, så `saksbehandler_ident is null` slår ikke til.
            repo.taBehandlingSaksbehandler(tattBehandling) shouldBe false

            // Vi tror en annen saksbehandler eier behandlingen enn den som faktisk står der.
            repo.overtaSaksbehandler(tattBehandling, "saksbehandlerSomAldriEide") shouldBe false
            repo.leggTilbakeSaksbehandler(tattBehandling, "saksbehandlerSomAldriEide") shouldBe false
        }
    }

    @Test
    fun `beslutter som taper kappløpet får false fra alle tildelingsmetodene`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, behandling) = opprettTilbakekrevingBehandlingTilGodkjenning(tac = tac)
            val (_, tattBehandling) = tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sak.id,
                tilbakekrevingId = behandling.id,
                saksbehandler = ObjectMother.beslutter("beslutterSomVant"),
            )!!
            val repo = tac.tilbakekrevingBehandlingRepo

            // Behandlingen har allerede en beslutter, så `beslutter_ident is null` slår ikke til.
            repo.taBehandlingBeslutter(tattBehandling) shouldBe false

            // Vi tror en annen beslutter eier behandlingen enn den som faktisk står der.
            repo.overtaBeslutter(tattBehandling, "beslutterSomAldriEide") shouldBe false
            repo.leggTilbakeBeslutter(tattBehandling, "beslutterSomAldriEide") shouldBe false
        }
    }
}
