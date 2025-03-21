package no.nav.tiltakspenger.saksbehandling.it

import arrow.core.Either
import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.april
import no.nav.tiltakspenger.saksbehandling.felles.mai
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.iverksett
import no.nav.tiltakspenger.saksbehandling.routes.routes
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
                    it.size shouldBe 1
                    it.single()
                    it.single().single().versjon shouldBe HendelseVersjon(1)
                }
                tac.kjørSendTilMeldekortApiJobb()
                tac.kjørGenererMeldeperiodeJobb().size shouldBe 0

                tac.clock.spolTil(23.april(2025))
                tac.kjørGenererMeldeperiodeJobb().size shouldBe 0

                tac.clock.spolTil(2.mai(2025))
                tac.kjørGenererMeldeperiodeJobb().size shouldBe 1
                tac.sakContext.sakService.hentForSakId(sak.id, ObjectMother.saksbehandler(), CorrelationId.generate())
                    .getOrFail().also {
                        it.meldeperiodeKjeder.size shouldBe 2
                        it.meldeperiodeKjeder.first().single().versjon.value shouldBe 1
                        it.meldeperiodeKjeder.last().single().versjon.value shouldBe 1
                    }

                // sanity jobb check
                tac.kjørGenererMeldeperiodeJobb().size shouldBe 0
                tac.sakContext.sakService.hentForSakId(sak.id, ObjectMother.saksbehandler(), CorrelationId.generate())
                    .getOrFail().also {
                        it.meldeperiodeKjeder.size shouldBe 2
                        it.meldeperiodeKjeder.first().single().versjon.value shouldBe 1
                        it.meldeperiodeKjeder.last().single().versjon.value shouldBe 1
                    }
            }
        }
    }
}

suspend fun TestApplicationContext.kjørSendTilMeldekortApiJobb() {
    return this.meldekortContext.sendMeldeperiodeTilBrukerService.send().also {
        this.meldekortContext.meldeperiodeRepo.hentUsendteTilBruker().size shouldBe 0
    }
}

fun TestApplicationContext.kjørGenererMeldeperiodeJobb(): List<Either<SakId, SakId>> {
    return this.genererMeldeperioderService.genererMeldeperioderForSaker()
}
