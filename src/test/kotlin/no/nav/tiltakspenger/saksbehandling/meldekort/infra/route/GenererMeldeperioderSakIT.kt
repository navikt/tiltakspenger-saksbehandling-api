package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

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
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.sendRevurderingInnvilgelseTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.taBehanding
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
                val (sak) = this.iverksettSøknadsbehandling(
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

    @Test
    fun `skal generere flere meldeperioder når innvilgelsesperioden forlenges fremover`() {
        val clock = TikkendeKlokke(fixedClockAt(22.april(2025)))
        with(TestApplicationContext(clock = clock)) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                val søknadsbehandlingInnvilgelse = Periode(1.april(2025), 13.april(2025))
                val revurderingInnvilgelse = søknadsbehandlingInnvilgelse.plusTilOgMed(28L)

                val (sak, _, søknadsbehandling, revurdering) = startRevurderingInnvilgelse(
                    tac,
                    søknadsbehandlingVirkningsperiode = søknadsbehandlingInnvilgelse,
                    revurderingVirkningsperiode = revurderingInnvilgelse,
                )

                sak.meldeperiodeKjeder.size shouldBe 1

                sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                    tac,
                    sak.id,
                    revurdering.id,
                    innvilgelsesperiode = revurderingInnvilgelse,
                    eksternDeltagelseId = søknadsbehandling.søknad.tiltak.id,
                )
                taBehanding(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
                val (oppdatertSak) = iverksettForBehandlingId(tac, sak.id, revurdering.id)

                oppdatertSak.meldeperiodeKjeder.let {
                    it.size shouldBe 3
                    it.all { kjede -> kjede.last().versjon == HendelseVersjon(1) } shouldBe true
                }
            }
        }
    }

    @Test
    fun `skal generere flere meldeperioder når innvilgelsesperioden forlenges bakover`() {
        val clock = TikkendeKlokke(fixedClockAt(22.april(2025)))
        with(TestApplicationContext(clock = clock)) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                val søknadsbehandlingInnvilgelse = Periode(1.april(2025), 13.april(2025))
                val revurderingInnvilgelse = søknadsbehandlingInnvilgelse.minusFraOgMed(14L)

                val (sak, _, søknadsbehandling, revurdering) = startRevurderingInnvilgelse(
                    tac,
                    søknadsbehandlingVirkningsperiode = søknadsbehandlingInnvilgelse,
                    revurderingVirkningsperiode = revurderingInnvilgelse,
                )

                sak.meldeperiodeKjeder.size shouldBe 1

                sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                    tac,
                    sak.id,
                    revurdering.id,
                    innvilgelsesperiode = revurderingInnvilgelse,
                    eksternDeltagelseId = søknadsbehandling.søknad.tiltak.id,
                )
                taBehanding(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
                val (oppdatertSak) = iverksettForBehandlingId(tac, sak.id, revurdering.id)

                oppdatertSak.meldeperiodeKjeder.let {
                    it.size shouldBe 2
                    it.first().last().versjon shouldBe HendelseVersjon(1)
                    it.last().last().versjon shouldBe HendelseVersjon(2)
                }
            }
        }
    }

    @Test
    fun `skal generere meldeperioder når innvilgelsesperioden forlenges fremover med hull midt i en meldeperiode`() {
        val clock = TikkendeKlokke(fixedClockAt(22.april(2025)))
        with(TestApplicationContext(clock = clock)) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                // Tirsdag til mandag (over to meldeperioder)
                val søknadsbehandlingInnvilgelse = Periode(1.april(2025), 14.april(2025))

                // Torsdag til søndag
                val revurderingInnvilgelse = Periode(17.april(2025), 20.april(2025))

                val (sak, _, søknadsbehandling, revurdering) = startRevurderingInnvilgelse(
                    tac,
                    søknadsbehandlingVirkningsperiode = søknadsbehandlingInnvilgelse,
                    revurderingVirkningsperiode = revurderingInnvilgelse,
                )

                sak.meldeperiodeKjeder.size shouldBe 2

                sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                    tac,
                    sak.id,
                    revurdering.id,
                    innvilgelsesperiode = revurderingInnvilgelse,
                    eksternDeltagelseId = søknadsbehandling.søknad.tiltak.id,
                )
                taBehanding(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
                val (oppdatertSak) = iverksettForBehandlingId(tac, sak.id, revurdering.id)

                oppdatertSak.meldeperiodeKjeder.let {
                    it.size shouldBe 2
                    it.first().last().versjon shouldBe HendelseVersjon(1)
                    val sisteMeldeperiode = it.last().last()
                    sisteMeldeperiode.versjon shouldBe HendelseVersjon(2)
                    sisteMeldeperiode.girRett shouldBe mapOf(
                        14.april(2025) to true,
                        15.april(2025) to false,
                        16.april(2025) to false,
                        17.april(2025) to true,
                        18.april(2025) to true,
                        19.april(2025) to true,
                        20.april(2025) to true,
                        21.april(2025) to false,
                        22.april(2025) to false,
                        23.april(2025) to false,
                        24.april(2025) to false,
                        25.april(2025) to false,
                        26.april(2025) to false,
                        27.april(2025) to false,
                    )
                    sisteMeldeperiode.antallDagerSomGirRett shouldBe 5
                }
            }
        }
    }
}
