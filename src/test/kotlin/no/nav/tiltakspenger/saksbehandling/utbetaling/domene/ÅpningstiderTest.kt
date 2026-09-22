package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider.erInnenforØkonomisystemetsÅpningstider
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals

class ÅpningstiderTest {
    val mandag = 1.desember(2025)

    @Test
    fun `er åpent i hverdager 06-21`() {
        for (dagOffset in DayOfWeek.MONDAY.ordinal..DayOfWeek.FRIDAY.ordinal) {
            val dag = mandag.plusDays(dagOffset.toLong())
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 6, minutt = 0)
            assertÅpningstid(forventetÅpent = true, dato = dag, time = 6, minutt = 10)
            assertÅpningstid(forventetÅpent = true, dato = dag, time = 6, minutt = 11)
            assertÅpningstid(forventetÅpent = true, dato = dag, time = 20, minutt = 49)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 20, minutt = 50)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 20, minutt = 51)
        }
    }

    @Test
    fun `stengt i helgen`() {
        for (offset in DayOfWeek.SATURDAY.ordinal..DayOfWeek.SUNDAY.ordinal) {
            val dag = mandag.plusDays(offset.toLong())
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 6, minutt = 0)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 6, minutt = 10)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 6, minutt = 11)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 20, minutt = 49)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 20, minutt = 50)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 20, minutt = 51)
        }
    }

    @Test
    fun `stengt på første juledag`() {
        val forsteJuledag = LocalDate.of(2025, 12, 25)

        assertÅpningstid(forventetÅpent = false, dato = forsteJuledag, time = 10, minutt = 0)
    }

    private fun assertÅpningstid(forventetÅpent: Boolean, dato: LocalDate, time: Int, minutt: Int) {
        val tidspunkt = dato.atTime(time, minutt)
        val clock = fixedClockAt(tidspunkt)
        assertEquals(
            forventetÅpent,
            erInnenforØkonomisystemetsÅpningstider(clock),
            "forventetÅpent=$forventetÅpent for ${dato.dayOfWeek} $tidspunkt",
        )
    }

    /** 5. mai 2025 er en mandag, 10. mai en lørdag, og 1. mai er en fast helligdag på en torsdag. */
    @Test
    fun `første åpne tidspunkt er tidspunktet selv når det er åpent, ellers neste åpning`() {
        val mandag = 5.mai(2025)

        Åpningstider.førsteÅpneTidspunkt(mandag.atTime(12, 0)) shouldBe mandag.atTime(12, 0)
        Åpningstider.førsteÅpneTidspunkt(mandag.atTime(6, 10)) shouldBe mandag.atTime(6, 10)
        Åpningstider.førsteÅpneTidspunkt(mandag.atTime(6, 9)) shouldBe mandag.atTime(6, 10)
        Åpningstider.førsteÅpneTidspunkt(mandag.atTime(20, 49)) shouldBe mandag.atTime(20, 49)
        Åpningstider.førsteÅpneTidspunkt(mandag.atTime(20, 50)) shouldBe mandag.plusDays(1).atTime(6, 10)
        Åpningstider.førsteÅpneTidspunkt(mandag.plusDays(5).atTime(12, 0)) shouldBe mandag.plusDays(7).atTime(6, 10)
        Åpningstider.førsteÅpneTidspunkt(1.mai(2025).atTime(12, 0)) shouldBe 2.mai(2025).atTime(6, 10)
    }

    @Test
    fun `neste åpne dag hopper over helg og fast helligdag`() {
        Åpningstider.nesteÅpneDag(5.mai(2025)) shouldBe 6.mai(2025)
        Åpningstider.nesteÅpneDag(9.mai(2025)) shouldBe 12.mai(2025)
        Åpningstider.nesteÅpneDag(30.april(2025)) shouldBe 2.mai(2025)
    }

    /** Fredag 2. mai er sendedag, mandag 5. mai er ventedag, og tirsdag 6. mai 06:10 er første åpning etter det. */
    @Test
    fun `en utbetaling kan stå i reskontroen to åpne dager etter at den ble sendt`() {
        Åpningstider.senesteSendetidspunktSomKanStåIReskontroen(6.mai(2025).atTime(6, 10)) shouldBe 2.mai(2025).atTime(20, 50)
        Åpningstider.senesteSendetidspunktSomKanStåIReskontroen(6.mai(2025).atTime(6, 9)) shouldBe 30.april(2025).atTime(20, 50)
        Åpningstider.senesteSendetidspunktSomKanStåIReskontroen(5.mai(2025).atTime(12, 0)) shouldBe 30.april(2025).atTime(20, 50)
        Åpningstider.senesteSendetidspunktSomKanStåIReskontroen(7.mai(2025).atTime(12, 0)) shouldBe 5.mai(2025).atTime(20, 50)
        Åpningstider.senesteSendetidspunktSomKanStåIReskontroen(10.mai(2025).atTime(12, 0)) shouldBe 7.mai(2025).atTime(20, 50)
    }
}
