package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import arrow.core.toNonEmptyListOrThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.clock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.meldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringerForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsdag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

private typealias DatoOgBeløp = Pair<LocalDate, Int>

class ValiderKanIverksetteUtbetalingTest {
    val periodeInnenforSammeMåned = Periode(13.januar(2025), 26.januar(2025))
    val periodePåTversAvMåned = Periode(27.januar(2025), 9.februar(2025))

    private fun simuleringMedJusteringer(
        vararg justeringer: Pair<Periode, List<DatoOgBeløp>>,
    ): Simulering {
        return Simulering.Endring(
            datoBeregnet = justeringer.first().first.tilOgMed,
            totalBeløp = 0,
            simuleringPerMeldeperiode = justeringer.toList().toNonEmptyListOrThrow().map {
                SimuleringForMeldeperiode(
                    meldeperiode = meldeperiode(periode = it.first),
                    simuleringsdager = it.second.toNonEmptyListOrThrow().map { (dato, beløp) ->
                        Simuleringsdag(
                            dato = dato,
                            tidligereUtbetalt = 0,
                            nyUtbetaling = 0,
                            totalEtterbetaling = 0,
                            totalFeilutbetaling = 0,
                            totalTrekk = 0,
                            totalJustering = beløp,
                            totalMotpostering = 0,
                            harJustering = true,
                            posteringsdag = PosteringerForDag(
                                dato = dato,
                                posteringer = nonEmptyListOf(
                                    PosteringForDag(
                                        dato = dato,
                                        fagområde = "TILTAKSPENGER",
                                        beløp = beløp,
                                        type = Posteringstype.JUSTERING,
                                        klassekode = "test_klassekode",
                                    ),
                                ),
                            ),
                        )
                    },
                )
            },
            simuleringstidspunkt = LocalDateTime.now(clock),
        )
    }

    @Test
    fun `Skal kunne iverksette utbetaling med justering innenfor samme meldeperiode og måned`() {
        val resultat = simuleringMedJusteringer(
            Pair(
                periodeInnenforSammeMåned,
                listOf(
                    Pair(periodeInnenforSammeMåned.fraOgMed, 1337),
                    Pair(periodeInnenforSammeMåned.tilOgMed, -1337),
                ),
            ),
        ).validerKanIverksetteUtbetaling()

        resultat.shouldBe(Unit.right())
    }

    @Test
    fun `Skal kunne iverksette utbetaling med justering innenfor samme meldeperiode og måned, selv om meldeperioden går på tvers av måneder`() {
        val resultat = simuleringMedJusteringer(
            Pair(
                periodeInnenforSammeMåned,
                listOf(
                    Pair(periodeInnenforSammeMåned.fraOgMed, 1337),
                    Pair(periodeInnenforSammeMåned.fraOgMed.plusDays(1), -1337),
                ),
            ),
        ).validerKanIverksetteUtbetaling()

        resultat.shouldBe(Unit.right())
    }

    @Test
    fun `Skal ikke kunne iverksette utbetaling med justering på tvers av meldeperioder`() {
        val resultat = simuleringMedJusteringer(
            Pair(
                periodeInnenforSammeMåned,
                listOf(
                    Pair(periodeInnenforSammeMåned.fraOgMed, 1337),
                ),
            ),
            Pair(
                periodePåTversAvMåned,
                listOf(
                    Pair(periodePåTversAvMåned.fraOgMed.plusDays(1), -1337),
                ),
            ),
        ).validerKanIverksetteUtbetaling()

        resultat.shouldBe(KanIkkeIverksetteUtbetaling.JusteringStøttesIkke.left())
    }

    @Test
    fun `Skal ikke kunne iverksette utbetaling med justering på tvers av måneder innenfor samme meldeperiode`() {
        val resultat = simuleringMedJusteringer(
            Pair(
                periodePåTversAvMåned,
                listOf(
                    Pair(periodePåTversAvMåned.fraOgMed, 1337),
                    Pair(periodePåTversAvMåned.tilOgMed, -1337),
                ),
            ),
        ).validerKanIverksetteUtbetaling()

        resultat.shouldBe(KanIkkeIverksetteUtbetaling.JusteringStøttesIkke.left())
    }
}
