package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.ta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehadlingLeggTilbakeOgTaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import org.junit.jupiter.api.Test

class TaMeldekortbehandlingRouteTest {
    @Test
    fun `beslutter kan ta meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, _) = this.iverksettSøknadsbehandlingOgBeslutterTarBehandling(tac)!!
            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING
        }
    }

    @Test
    fun `saksbehandler kan ta meldekortbehandling som er lagt tilbake`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, _) = this.iverksettSøknadsbehadlingLeggTilbakeOgTaMeldekortbehandling(tac)!!
            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
        }
    }

    @Test
    fun `returnerer 400 når meldekortbehandlingen har ugyldig status`() {
        withTestApplicationContext { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandler")
            val annenSaksbehandler = ObjectMother.saksbehandler("annenSaksbehandler")
            val (_, _, rammevedtak, meldekortbehandling, _) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
                tac = tac,
                saksbehandler = saksbehandler,
            )!!

            // Meldekortbehandlingen er UNDER_BEHANDLING, så den kan ikke tas.
            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING

            taMeldekortbehanding(
                tac = tac,
                sakId = rammevedtak.sakId,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = annenSaksbehandler,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )
        }
    }

    @Test
    fun `returnerer 400 når beslutter er samme som saksbehandler på behandlingen`() {
        withTestApplicationContext { tac ->
            val saksbehandlerOgBeslutter = ObjectMother.saksbehandlerOgBeslutter()
            val (_, _, rammevedtak, meldekortbehandling, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(
                tac = tac,
                saksbehandler = saksbehandlerOgBeslutter,
            )!!

            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING

            taMeldekortbehanding(
                tac = tac,
                sakId = rammevedtak.sakId,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = saksbehandlerOgBeslutter,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )
        }
    }

    @Test
    fun `returnerer 404 når meldekortbehandlingen ikke finnes på saken`() {
        withTestApplicationContext { tac ->
            val (_, _, rammevedtak, _) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!

            taMeldekortbehanding(
                tac = tac,
                sakId = rammevedtak.sakId,
                meldekortId = MeldekortId.random(),
                forventet = ForventetRespons(404, contentType = "application/json; charset=UTF-8"),
            )
        }
    }
}
