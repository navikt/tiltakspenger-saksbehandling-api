package no.nav.tiltakspenger.saksbehandling.sak

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SaksnummerTest {

    @Test
    fun prefiks() {
        val saksnummer = Saksnummer.genererSaknummer(dato = LocalDate.of(2021, 1, 1), løpenr = "1001")
        saksnummer.verdi shouldBe "202101011001"
        saksnummer.løpenr shouldBe "1001"
        saksnummer.prefiks shouldBe "20210101"
        saksnummer.dato shouldBe LocalDate.of(2021, 1, 1)
    }

    @Test
    fun `inkrementerer prod med 1`() {
        val saksnummer = Saksnummer.genererSaknummer(dato = LocalDate.of(2021, 1, 1), løpenr = "0001")
        saksnummer.verdi shouldBe "202101010001"
        saksnummer.løpenr shouldBe "0001"
        saksnummer.prefiks shouldBe "20210101"
        saksnummer.dato shouldBe LocalDate.of(2021, 1, 1)
        val neste = saksnummer.nesteSaksnummer()
        neste.verdi shouldBe "202101010002"
        neste.løpenr shouldBe "0002"
        neste.prefiks shouldBe "20210101"
        neste.dato shouldBe LocalDate.of(2021, 1, 1)
    }
}
