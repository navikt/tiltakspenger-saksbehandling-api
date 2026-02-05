package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingInnvilgelse
import org.junit.jupiter.api.Test

class GenererMeldeperioderSakIT {

    @Test
    fun `skal generere meldeperioder for sak som har vedtak fram i tid når vi er ferdig med meldeperiodesyklusen`() {
        val clock = TikkendeKlokke(fixedClockAt(22.april(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val fnr = Fnr.random()
            val (sak) = this.iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                /*
                 Totalt skal dette føre til 2 meldeperioder
                 7 april - 20 april
                 21 april - 4 mai (genereres ikke før 2.mai)
                 */
                innvilgelsesperioder = innvilgelsesperioder(Periode(9.april(2025), 1.mai(2025))),
            )
            sak.meldeperiodeKjeder.let {
                it.size shouldBe 2
                it.last().last().versjon shouldBe HendelseVersjon(1)
            }
        }
    }

    @Test
    fun `skal generere flere meldeperioder når innvilgelsesperioden forlenges fremover`() {
        val clock = TikkendeKlokke(fixedClockAt(22.april(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val søknadsbehandlingInnvilgelse = Periode(1.april(2025), 13.april(2025))
            val revurderingInnvilgelse = søknadsbehandlingInnvilgelse.plusTilOgMed(28L)

            val (sak) = this.iverksettSøknadsbehandlingOgRevurderingInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(søknadsbehandlingInnvilgelse),
                revurderingInnvilgelsesperioder = innvilgelsesperioder(revurderingInnvilgelse),
            )
            sak.meldeperiodeKjeder.let {
                it.size shouldBe 3
                it.all { kjede -> kjede.last().versjon == HendelseVersjon(1) } shouldBe true
            }
        }
    }

    @Test
    fun `skal generere flere meldeperioder når innvilgelsesperioden forlenges bakover`() {
        val clock = TikkendeKlokke(fixedClockAt(22.april(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            // 1. april 2025 var en tirsdag, 13. april en søndag.
            val søknadsbehandlingInnvilgelse = 1.april(2025).til(13.april(2025))
            // 18. mars 2025 var en tirsdag, 13. april en søndag.
            val revurderingInnvilgelse = 18.mars(2025).til(13.april(2025))

            val (sak) = this.iverksettSøknadsbehandlingOgRevurderingInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(søknadsbehandlingInnvilgelse),
                revurderingInnvilgelsesperioder = innvilgelsesperioder(revurderingInnvilgelse),
            )
            sak.meldeperiodeKjeder.let {
                it.size shouldBe 2
                it.first().last().versjon shouldBe HendelseVersjon(1)
                // Mandag 31. inkluderes i meldeperioden til søknadsbehandlingen, revurderingen gjør at den går fra gir ikke rett til rett og bumper versjonen.
                it.last().last().versjon shouldBe HendelseVersjon(2)
            }
        }
    }

    @Test
    fun `skal generere meldeperioder når innvilgelsesperioden forlenges fremover med hull midt i en meldeperiode`() {
        val clock = TikkendeKlokke(fixedClockAt(22.april(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            // Tirsdag til mandag (over to meldeperioder)
            val søknadsbehandlingInnvilgelse = Periode(1.april(2025), 14.april(2025))

            // Torsdag til søndag
            val revurderingInnvilgelse = Periode(17.april(2025), 20.april(2025))

            val (sak) = this.iverksettSøknadsbehandlingOgRevurderingInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(søknadsbehandlingInnvilgelse),
                revurderingInnvilgelsesperioder = innvilgelsesperioder(revurderingInnvilgelse),
            )

            sak.meldeperiodeKjeder.let {
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

    @Test
    fun `skal generere meldeperioder for to innvilgede søknader med hull mellom, uten meldeperioder i hullet`() {
        val clock = TikkendeKlokke(fixedClockAt(22.april(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val fnr = Fnr.random()
            val (sak) = this.iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                // To meldeperioder i denne perioden
                innvilgelsesperioder = innvilgelsesperioder(Periode(7.april(2025), 21.april(2025))),
                beslutter = ObjectMother.beslutter(),
            )

            sak.meldeperiodeKjeder.let {
                it.size shouldBe 2
                it.alleMeldeperioder.size shouldBe 2
                it.alleMeldeperioder.forEach { mp -> mp.versjon shouldBe HendelseVersjon(1) }
            }

            val (oppdatertSak) = this.iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                // 4 meldeperioder i denne perioden (første fra 19.mai - 1.juni)
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.juni(2025), 1.juli(2025))),
                beslutter = ObjectMother.beslutter(),
            )

            oppdatertSak.meldeperiodeKjeder.let {
                it.size shouldBe 6
                it.alleMeldeperioder.size shouldBe 6
                it.alleMeldeperioder.forEach { mp -> mp.versjon shouldBe HendelseVersjon(1) }
            }
        }
    }
}
