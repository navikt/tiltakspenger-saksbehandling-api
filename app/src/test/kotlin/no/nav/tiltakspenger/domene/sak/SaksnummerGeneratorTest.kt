package no.nav.tiltakspenger.domene.sak

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.SaksnummerGenerator
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SaksnummerGeneratorTest {

    private val dato = LocalDate.of(2021, 1, 1)

    @Test
    fun prod() {
        val saksnummerGenerator = SaksnummerGenerator.Prod
        val saksnummer = saksnummerGenerator.generer(dato)
        saksnummer.verdi shouldBe "202101010001"
        saksnummer.løpenr shouldBe "0001"
        saksnummer.prefiks shouldBe "20210101"
        saksnummer.dato shouldBe LocalDate.of(2021, 1, 1)
    }

    @Test
    fun dev() {
        val saksnummerGenerator = SaksnummerGenerator.Dev
        val saksnummer = saksnummerGenerator.generer(dato)
        saksnummer.verdi shouldBe "202101011001"
        saksnummer.løpenr shouldBe "1001"
        saksnummer.prefiks shouldBe "20210101"
        saksnummer.dato shouldBe LocalDate.of(2021, 1, 1)
    }

    @Test
    fun local() {
        val saksnummerGenerator = SaksnummerGenerator.Local
        val saksnummer = saksnummerGenerator.generer(dato)
        saksnummer.verdi shouldBe "202101011001"
        saksnummer.løpenr shouldBe "1001"
        saksnummer.prefiks shouldBe "20210101"
        saksnummer.dato shouldBe LocalDate.of(2021, 1, 1)
    }
}
