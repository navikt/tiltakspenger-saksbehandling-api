package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.avbryt

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgAvbrytMeldekortbehandling
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
}
