package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider.erInnenforØkonomisystemetsÅpningstider
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals

class ÅpningstiderTest {
    @Test
    fun `er åpent i hverdager 06-21`() {
        val nå = LocalDate.now()
        val mandag = nå.with(DayOfWeek.MONDAY)

        for (dagOffset in DayOfWeek.MONDAY.ordinal..DayOfWeek.FRIDAY.ordinal) {
            val dag = mandag.plusDays(dagOffset.toLong())
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 5, minutt = 59)
            assertÅpningstid(forventetÅpent = true, dato = dag, time = 6, minutt = 0)
            assertÅpningstid(forventetÅpent = true, dato = dag, time = 6, minutt = 1)
            assertÅpningstid(forventetÅpent = true, dato = dag, time = 20, minutt = 59)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 21, minutt = 0)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 21, minutt = 1)
        }
    }

    @Test
    fun `stengt i helgen`() {
        val nå = LocalDate.now()
        val mandag = nå.with(DayOfWeek.MONDAY)

        for (offset in DayOfWeek.SATURDAY.ordinal..DayOfWeek.SUNDAY.ordinal) {
            val dag = mandag.plusDays(offset.toLong())
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 5, minutt = 59)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 6, minutt = 0)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 6, minutt = 1)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 20, minutt = 59)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 21, minutt = 0)
            assertÅpningstid(forventetÅpent = false, dato = dag, time = 21, minutt = 1)
        }
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
}
