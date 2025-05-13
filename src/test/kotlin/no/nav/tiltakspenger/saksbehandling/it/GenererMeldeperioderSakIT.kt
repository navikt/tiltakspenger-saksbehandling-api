package no.nav.tiltakspenger.saksbehandling.it

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.libs.periodisering.mai
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.iverksett
import org.junit.jupiter.api.Test

class GenererMeldeperioderSakIT {

    @Test
    fun `skal generere meldeperioder for sak som har vedtak fram i tid når vi er ferdig med meldeperiodesyklusen`() {
        val clock = TikkendeKlokke(fixedClockAt(22.april(2025)))
        with(TestApplicationContext(clock = clock)) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                val fnr = Fnr.random()
                val (sak) = this.iverksett(
                    tac = tac,
                    fnr = fnr,
                    /**
                     * Totalt skal dette føre til 2 meldeperioder
                     * 7 april - 20 april
                     * 21 april - 4 mai (genereres ikke før 2.mai)
                     */
                    virkingsperiode = Periode(9.april(2025), 1.mai(2025)),
                    beslutter = ObjectMother.beslutter(),
                )
                sak.meldeperiodeKjeder.let {
                    it.size shouldBe 2
                    it.last().last().versjon shouldBe HendelseVersjon(1)
                }
            }
        }
    }
}
