package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Feilutbetaling og motpostering.
 *
 * Disse to følges alltid ad i dataene: en positiv feilutbetaling kommer med en negativ motpostering av samme størrelse, og motsatt.
 * 757 av dagene i dev-uttrekket hadde nettopp denne formen.
 */
internal class OppsummeringGeneratorFeilutbetalingTest {

    @Test
    fun `positiv feilutbetaling med motpostering`() {
        val dag = simulerDag(
            ytelse(312),
            ytelse(112),
            ytelse(-312),
            ytelse(-112),
            feilutbetaling(424),
            motpostering(-424),
        )

        dag.tidligereUtbetalt shouldBe 424
        dag.totalFeilutbetaling shouldBe 424
        dag.totalMotpostering shouldBe -424
        // Feilutbetalingen trekkes fra de positive ytelsene.
        dag.nyUtbetaling shouldBe 0
        dag.totalEtterbetaling shouldBe 0
        dag.harFeilutbetaling shouldBe true
    }

    @Test
    fun `feilutbetaling reduserer ny utbetaling`() {
        val dag = simulerDag(
            ytelse(110),
            ytelse(55),
            ytelse(-110),
            feilutbetaling(55),
            motpostering(-55),
        )

        dag.tidligereUtbetalt shouldBe 110
        dag.nyUtbetaling shouldBe 110
        dag.totalFeilutbetaling shouldBe 55
    }

    /**
     * Negative feilutbetalinger forekommer -- 53 posteringer i dev-uttrekket -- men de teller ikke.
     *
     * `beregnFeilutbetaling` summerer bare de positive, og `beregnNyttBeløp` gjør det samme.
     * Motposteringen er derimot positiv og summeres uansett fortegn.
     */
    @Test
    fun `negativ feilutbetaling gir null feilutbetaling`() {
        val dag = simulerDag(
            ytelse(312),
            ytelse(112),
            ytelse(-312),
            ytelse(-112),
            feilutbetaling(-424),
            motpostering(424),
        )

        dag.totalFeilutbetaling shouldBe 0
        dag.totalMotpostering shouldBe 424
        dag.nyUtbetaling shouldBe 424
        dag.harFeilutbetaling shouldBe false
    }

    @Test
    fun `motposteringer summeres uansett fortegn`() {
        val dag = simulerDag(ytelse(400), motpostering(-100), motpostering(30))

        dag.totalMotpostering shouldBe -70
    }

    @Test
    fun `feilutbetaling uten motpostering påvirker bare feilutbetalingen`() {
        val dag = simulerDag(ytelse(400), feilutbetaling(100))

        dag.totalFeilutbetaling shouldBe 100
        dag.totalMotpostering shouldBe 0
        dag.nyUtbetaling shouldBe 300
    }

    @Test
    fun `feilutbetaling som overstiger ytelsen gir negativ ny utbetaling`() {
        val dag = simulerDag(ytelse(100), feilutbetaling(150))

        // Merk at nyUtbetaling ikke er gulvet på 0, i motsetning til etterbetaling og feilutbetaling.
        dag.nyUtbetaling shouldBe -50
        dag.totalEtterbetaling shouldBe 0
    }
}
