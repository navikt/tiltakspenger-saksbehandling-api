package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.leggTilbake

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgLeggTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggTilbakeMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import org.junit.jupiter.api.Test

class LeggTilbakeMeldekortbehandlingRouteTest {
    @Test
    fun `beslutter kan legge tilbake meldekortbehandling`() {
//        withTestApplicationContext { tac ->
//            val (sak, _, _,meldekortbehandling) = this.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgLeggTilbake(
//                tac = tac
//            )
//            val beslutterIdent = "Z12345"
//            val beslutter = ObjectMother.beslutter(navIdent = beslutterIdent)
//            val meldekortBehandling = ObjectMother.meldekortBehandletManuelt(
//                sakId = sak.id,
//                saksnummer = sak.saksnummer,
//                fnr = sak.fnr,
//                beslutter = null,
//                status = MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
//                iverksattTidspunkt = null,
//            )
//            tac.meldekortContext.meldekortBehandlingRepo.lagre(meldekortBehandling, null)
//
//            taMeldekortbehanding(tac, meldekortBehandling.sakId, meldekortBehandling.id, beslutter).also {
//                val oppdatertMeldekortbehandling =
//                    tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortBehandling.id)
//                oppdatertMeldekortbehandling shouldNotBe null
//                oppdatertMeldekortbehandling?.status shouldBe MeldekortBehandlingStatus.UNDER_BESLUTNING
//                oppdatertMeldekortbehandling?.beslutter shouldBe beslutterIdent
//            }
//
//            leggTilbakeMeldekortbehandling(
//                tac = tac,
//                sakId = meldekortBehandling.sakId,
//                meldekortId = meldekortBehandling.id,
//                saksbehandler = beslutter,
//            ).also {
//                val oppdatertMeldekortbehandling =
//                    tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortBehandling.id)
//                oppdatertMeldekortbehandling shouldNotBe null
//                oppdatertMeldekortbehandling?.status shouldBe MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
//                oppdatertMeldekortbehandling?.beslutter shouldBe null
//            }
//        }
    }

    @Test
    fun `saksbehandler kan legge tilbake meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(tac)
            val saksbehandlerIdent = "Z12345"
            val saksbehandler = ObjectMother.saksbehandler(navIdent = saksbehandlerIdent)
            val meldekortBehandling = ObjectMother.meldekortUnderBehandling(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                saksbehandler = saksbehandlerIdent,
                status = MeldekortBehandlingStatus.UNDER_BEHANDLING,
            )
            tac.meldekortContext.meldekortBehandlingRepo.lagre(meldekortBehandling, null)

            leggTilbakeMeldekortbehandling(tac, meldekortBehandling.sakId, meldekortBehandling.id, saksbehandler).also {
                val oppdatertMeldekortbehandling =
                    tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortBehandling.id)
                oppdatertMeldekortbehandling shouldNotBe null
                oppdatertMeldekortbehandling?.status shouldBe MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING
                oppdatertMeldekortbehandling?.saksbehandler shouldBe null
            }
        }
    }
}
