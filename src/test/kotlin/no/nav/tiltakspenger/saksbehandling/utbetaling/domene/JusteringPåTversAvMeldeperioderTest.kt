package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.dato.januar
import org.junit.jupiter.api.Test
import java.time.YearMonth

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
     * Samme justeringsbeløp, men den positive og den negative ligger i hver sin meldeperiode -- formen alle de fem justeringssakene i dev-uttrekket hadde.
     *
     * Oppdrag omfordeler forskuddstrekk per kalendermåned, med justeringer som motpost på tvers av meldeperiodene.
     * Balanserer justeringene i måneden og det ikke er feilutbetaling, er månedens utbetaling uendret -- da tillates de, og saksbehandler advares i visningen.
     * Jf. dev-casen i [TrekkMedJusteringFraDevTest].
     */
    @Test
    fun `justering som krysser meldeperiodegrensen tillates når den balanserer i kalendermåneden`() {
        val simulering = simuleringForDager(
            6.januar(2025) to listOf(ytelse(400), justering(106)),
            20.januar(2025) to listOf(ytelse(400), justering(-106)),
        )

        simulering.simuleringPerMeldeperiode.size shouldBe 2
        // Justeringene nuller hverandre ut i januar, men ikke innenfor hver meldeperiode.
        simulering.totalJustering shouldBe 0
        simulering.simuleringPerMeldeperiode.all { it.harUbalansertJustering } shouldBe true

        simulering.validerKanIverksetteUtbetaling().isRight() shouldBe true
    }

    /** Med feilutbetaling i simuleringen er vi i motregnings-/kravgrunnlagsklassen, og da sperres krysningen fortsatt. */
    @Test
    fun `justering på tvers sperres når simuleringen også har feilutbetaling`() {
        val simulering = simuleringForDager(
            6.januar(2025) to listOf(ytelse(400), justering(106)),
            20.januar(2025) to listOf(ytelse(400), justering(-106), feilutbetaling(50), motpostering(-50)),
        )

        val feil = simulering.validerKanIverksetteUtbetaling()
            .leftOrNull()
            .shouldBeInstanceOf<KanIkkeIverksetteUtbetaling.JusteringStøttesIkke>()

        feil.ubalanserte.map { it.beløpPerMåned } shouldBe listOf(
            mapOf(YearMonth.of(2025, 1) to 106),
            mapOf(YearMonth.of(2025, 1) to -106),
        )
        // Meldingen skal si hvor og hvor mye, slik at saksbehandler slipper å gjette.
        feil.beskrivelse shouldContain "06.01.2025–19.01.2025 (+106 kr i januar)"
        feil.beskrivelse shouldContain "20.01.2025–02.02.2025 (−106 kr i januar)"
    }

    /**
     * Flytting av selve ytelsen skal fortsatt sperres, selv når justeringene balanserer i måneden uten feilutbetaling.
     *
     * Casen: en utbetalt dag korrigeres bort i første meldeperiode og en dag går fra annet fravær til deltatt i den andre.
     * Oppdrag motregner innenfor måneden -- justeringene går opp i null -- men ytelsen er flyttet mellom meldeperiodene, og det har vi ikke hjemmel til.
     * Kjennetegnet er den reverserte ytelsen (negativ YTELSE-postering) i meldeperioden med ubalansert justering.
     */
    @Test
    fun `flyttet ytelse sperres selv når justeringene balanserer i måneden`() {
        val simulering = simuleringForDager(
            // Første meldeperiode: den utbetalte dagen reverseres, og justeringen dekker den i stedet for feilutbetaling.
            6.januar(2025) to listOf(ytelse(-298), justering(298)),
            // Andre meldeperiode: dagen som gikk fra annet fravær til deltatt.
            20.januar(2025) to listOf(ytelse(298), justering(-298)),
        )

        simulering.ubalanserteJusteringsmåneder shouldBe emptyMap()
        simulering.harFeilutbetaling shouldBe false
        simulering.simuleringPerMeldeperiode.first().harReversertYtelse shouldBe true

        simulering.validerKanIverksetteUtbetaling()
            .leftOrNull()
            .shouldBeInstanceOf<KanIkkeIverksetteUtbetaling.JusteringStøttesIkke>()
    }

    /** Regelen gjelder uansett hvilken posteringstype justeringen kommer under -- klassekoden avgjør. */
    @Test
    fun `balansert krysning tillates også når justeringen kommer som feilutbetaling med justeringsklassekode`() {
        val simulering = simuleringForDager(
            6.januar(2025) to listOf(ytelse(400), feilutbetalingMedJusteringsklassekode(106)),
            20.januar(2025) to listOf(ytelse(400), feilutbetalingMedJusteringsklassekode(-106)),
        )

        simulering.validerKanIverksetteUtbetaling().isRight() shouldBe true
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
