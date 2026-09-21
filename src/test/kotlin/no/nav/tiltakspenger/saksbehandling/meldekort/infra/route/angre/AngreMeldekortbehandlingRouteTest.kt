package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.angre

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.angreMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import org.junit.jupiter.api.Test

class AngreMeldekortbehandlingRouteTest {

    @Test
    fun `en saksbehandler kan angre en meldekortbehandling sendt til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac)!!

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
                it.beslutter shouldBe null
            }

            angreMeldekortbehandling(
                tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
            )!!.also { (_, angretMeldekortbehandling, _) ->
                angretMeldekortbehandling?.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
                angretMeldekortbehandling?.sendtTilBeslutning shouldBe null
                tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                    it.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
                    it.sendtTilBeslutning shouldBe null
                }
            }
        }
    }
}
