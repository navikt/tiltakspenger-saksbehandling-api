package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.toggleKanSendeHelgForMeldekort
import org.junit.jupiter.api.Test

/**
 * Kjører mot postgres fordi ruta er eneste prodsti til `SakPostgresRepo.oppdaterKanSendeInnHelgForMeldekort`.
 */
class ToggleKanSendeHelgForMeldekortSakRouteTest {

    @Test
    fun `saksbehandler kan slå muligheten til å melde helg av og på`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _) = opprettSakOgSøknad(tac)
            val sakRepo = tac.sakContext.sakRepo

            sakRepo.hentForSakId(sak.id)!!.kanSendeInnHelgForMeldekort shouldBe false

            toggleKanSendeHelgForMeldekort(tac, sak.id, kanSendeHelg = true)
            sakRepo.hentForSakId(sak.id)!!.kanSendeInnHelgForMeldekort shouldBe true

            toggleKanSendeHelgForMeldekort(tac, sak.id, kanSendeHelg = false)
            sakRepo.hentForSakId(sak.id)!!.kanSendeInnHelgForMeldekort shouldBe false
        }
    }
}
