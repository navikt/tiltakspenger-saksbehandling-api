package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.avbryt

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgAvbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgAvbrytMeldekortbehandling
import org.junit.jupiter.api.Test

class AvbrytMeldekortbehandlingRouteTest {

    @Test
    fun `saksbehandler kan avbryte meldekortbehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val (_, _, _, avbruttMeldekortbehandling) = this.iverksettSøknadsbehandlingOgAvbrytMeldekortbehandling(
                tac = tac,
            )!!

            avbruttMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.AVBRUTT
        }
    }

    @Test
    fun `kan avbryte to meldekortbehandlinger på samme kjede`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val (sakMedFørsteAvbrutteMeldekortbehandling, _, _, avbruttMeldekortbehandling, _) = this.iverksettSøknadsbehandlingOgAvbrytMeldekortbehandling(
                tac = tac,
            )!!
            val sakId = sakMedFørsteAvbrutteMeldekortbehandling.id
            val kjedeId = avbruttMeldekortbehandling.meldeperioder.first().meldeperiode.kjedeId
            val (oppdatertSak) = this.opprettOgAvbrytMeldekortbehandling(
                tac = tac,
                sakId = sakId,
                kjedeId = kjedeId,
            )!!
            oppdatertSak.meldekortbehandlinger.also { meldekortbehandlinger ->
                meldekortbehandlinger.size shouldBe 2
                meldekortbehandlinger.forEach { it.status shouldBe MeldekortbehandlingStatus.AVBRUTT }
            }
        }
    }

    @Test
    fun `saksbehandler kan avbryte meldekortbehandling som er klar til beslutning`() {
        val saksbehandler = ObjectMother.saksbehandler()
        withTestApplicationContext { tac ->
            val (sak, _, _, meldekortbehandling) = this.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(
                tac = tac,
                saksbehandler = saksbehandler,
            )!!

            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING

            val (_, avbruttMeldekortbehandling) = this.avbrytMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler,
            )!!

            avbruttMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.AVBRUTT
        }
    }

    @Test
    fun `beslutter kan avbryte meldekortbehandling som er under beslutning`() {
        val saksbehandler = ObjectMother.saksbehandler(navIdent = "saksbehandler")
        val beslutter = ObjectMother.saksbehandlerOgBeslutter(navIdent = "beslutter")
        withTestApplicationContext { tac ->
            val (sak, _, _, meldekortbehandling) = this.iverksettSøknadsbehandlingOgBeslutterTarBehandling(
                tac = tac,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
            )!!

            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING

            val (_, avbruttMeldekortbehandling) = this.avbrytMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = beslutter,
            )!!

            avbruttMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.AVBRUTT
        }
    }

    @Test
    fun `saksbehandler kan ikke avbryte meldekortbehandling som er under beslutning hos en annen beslutter`() {
        val saksbehandler = ObjectMother.saksbehandlerOgBeslutter(navIdent = "saksbehandler")
        val beslutter = ObjectMother.saksbehandlerOgBeslutter(navIdent = "beslutter")
        withTestApplicationContext { tac ->
            val (sak, _, _, meldekortbehandling) = this.iverksettSøknadsbehandlingOgBeslutterTarBehandling(
                tac = tac,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
            )!!

            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING

            this.avbrytMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )
        }
    }
}
