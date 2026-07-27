package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import org.junit.jupiter.api.Test

/**
 * Hjemmelsvernet mot justering på tvers av meldeperioder.
 *
 * Oppdrag balanserer negative og positive dager mot hverandre innenfor en kalendermåned.
 * Vi har ikke hjemmel til å la en slik justering gå på tvers av meldeperioder, så
 * `harJusteringPåTversAvMeldeperioderEllerMåneder` blokkerer iverksetting når det skjer, og saksbehandler må ta stilling til saken.
 *
 * Regelen ser på summen av justeringer per måned **innenfor hver enkelt meldeperiode**.
 * Går summen opp i null overalt, er justeringen innelukket der den hører hjemme, og iverksetting skal gå gjennom -- også når flere meldeperioder har justeringer hver for seg.
 *
 * Testene bygger simuleringen fra posteringer, slik at hele veien fra helved-responsen til vernet blir kjørt.
 */
internal class JusteringPåTversAvMeldeperioderTest {

    /**
     * To meldeperioder, med justeringer som balanserer i null i hver av dem.
     *
     * Ingen av justeringene krysser en meldeperiodegrense, så dette er en balansering oppdrag har lov til å gjøre.
     */
    @Test
    fun `justeringer som balanserer internt i to meldeperioder slipper gjennom`() {
        val simulering = simuleringForDager(
            6.januar(2025) to listOf(ytelse(400), justering(106)),
            7.januar(2025) to listOf(ytelse(400), justering(-106)),
            20.januar(2025) to listOf(ytelse(400), justering(212)),
            21.januar(2025) to listOf(ytelse(400), justering(-212)),
        )

        simulering.simuleringPerMeldeperiode.size shouldBe 2
        simulering.simuleringPerMeldeperiode.forAll { it.harJustering shouldBe true }
        simulering.totalJustering shouldBe 0

        simulering.validerKanIverksetteUtbetaling().isRight() shouldBe true
    }

    /** Samme innenfor én meldeperiode: balanserer justeringen der, skal den gå gjennom. */
    @Test
    fun `justeringer som balanserer internt i én meldeperiode slipper gjennom`() {
        val simulering = simuleringForDager(
            6.januar(2025) to listOf(ytelse(400), justering(106)),
            7.januar(2025) to listOf(ytelse(400), justering(-106)),
        )

        simulering.simuleringPerMeldeperiode.size shouldBe 1
        simulering.validerKanIverksetteUtbetaling().isRight() shouldBe true
    }

    /**
     * Kontrasten: samme justeringsbeløp, men den positive og den negative ligger i hver sin meldeperiode.
     *
     * Da går ingen av meldeperiodene opp i null, og iverksetting blokkeres.
     * Dette er formen alle de fem justeringssakene i dev-uttrekket hadde.
     */
    @Test
    fun `justering som krysser meldeperiodegrensen blokkeres`() {
        val simulering = simuleringForDager(
            6.januar(2025) to listOf(ytelse(400), justering(106)),
            20.januar(2025) to listOf(ytelse(400), justering(-106)),
        )

        simulering.simuleringPerMeldeperiode.size shouldBe 2
        // Justeringene nuller hverandre ut totalt, men ikke innenfor hver meldeperiode.
        simulering.totalJustering shouldBe 0

        simulering.validerKanIverksetteUtbetaling()
            .leftOrNull() shouldBe KanIkkeIverksetteUtbetaling.JusteringStøttesIkke
    }

    /** Vernet gjelder uansett hvilken posteringstype justeringen kommer under. */
    @Test
    fun `krysningen fanges også når justeringen kommer som feilutbetaling`() {
        val simulering = simuleringForDager(
            6.januar(2025) to listOf(ytelse(400), feilutbetalingMedJusteringsklassekode(106)),
            20.januar(2025) to listOf(ytelse(400), feilutbetalingMedJusteringsklassekode(-106)),
        )

        simulering.validerKanIverksetteUtbetaling()
            .leftOrNull() shouldBe KanIkkeIverksetteUtbetaling.JusteringStøttesIkke
    }

    /** Og den balanserte varianten slipper gjennom med den formen også. */
    @Test
    fun `balansert justering som feilutbetaling slipper gjennom`() {
        val simulering = simuleringForDager(
            6.januar(2025) to listOf(ytelse(400), feilutbetalingMedJusteringsklassekode(106)),
            7.januar(2025) to listOf(ytelse(400), feilutbetalingMedJusteringsklassekode(-106)),
            20.januar(2025) to listOf(ytelse(400), feilutbetalingMedJusteringsklassekode(212)),
            21.januar(2025) to listOf(ytelse(400), feilutbetalingMedJusteringsklassekode(-212)),
        )

        simulering.simuleringPerMeldeperiode.size shouldBe 2
        simulering.validerKanIverksetteUtbetaling().isRight() shouldBe true
    }

    /** En simulering uten justeringer i det hele tatt skal aldri treffe vernet. */
    @Test
    fun `simulering uten justering slipper gjennom`() {
        val simulering = simuleringForDager(
            6.januar(2025) to listOf(ytelse(400)),
            20.januar(2025) to listOf(ytelse(400)),
        )

        simulering.simuleringPerMeldeperiode.forAll { it.harJustering shouldBe false }
        simulering.validerKanIverksetteUtbetaling().isRight() shouldBe true
    }
}

private fun <T> Iterable<T>.forAll(block: (T) -> Unit) = forEach(block)
