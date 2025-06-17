package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortBehanding
import org.junit.jupiter.api.Test

class TaMeldekortBehandlingRouteTest {
    @Test
    fun `beslutter kan ta meldekortbehandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _) = this.iverksettSøknadsbehandling(tac)
                val beslutterIdent = "Z12345"
                val beslutter = ObjectMother.beslutter(navIdent = beslutterIdent)
                val meldekortBehandling = ObjectMother.meldekortBehandletManuelt(
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = sak.fnr,
                    beslutter = null,
                    status = MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
                    iverksattTidspunkt = null,
                )

                tac.meldekortContext.meldekortBehandlingRepo.lagre(meldekortBehandling, null)

                taMeldekortBehanding(tac, meldekortBehandling.sakId, meldekortBehandling.id, beslutter).also {
                    val oppdatertMeldekortbehandling = tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortBehandling.id)
                    oppdatertMeldekortbehandling shouldNotBe null
                    oppdatertMeldekortbehandling?.status shouldBe MeldekortBehandlingStatus.UNDER_BESLUTNING
                    oppdatertMeldekortbehandling?.beslutter shouldBe beslutterIdent
                }
            }
        }
    }

    @Test
    fun `saksbehandler kan ta meldekortbehandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _) = this.iverksettSøknadsbehandling(tac)
                val saksbehandlerIdent = "Z12345"
                val saksbehandler = ObjectMother.saksbehandler(navIdent = saksbehandlerIdent)
                val meldekortBehandling = ObjectMother.meldekortUnderBehandling(
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = sak.fnr,
                    saksbehandler = null,
                    status = MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING,
                )

                tac.meldekortContext.meldekortBehandlingRepo.lagre(meldekortBehandling, null)

                taMeldekortBehanding(tac, meldekortBehandling.sakId, meldekortBehandling.id, saksbehandler).also {
                    val oppdatertMeldekortbehandling = tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortBehandling.id)
                    oppdatertMeldekortbehandling shouldNotBe null
                    oppdatertMeldekortbehandling?.status shouldBe MeldekortBehandlingStatus.UNDER_BEHANDLING
                    oppdatertMeldekortbehandling?.saksbehandler shouldBe saksbehandlerIdent
                }
            }
        }
    }
}
