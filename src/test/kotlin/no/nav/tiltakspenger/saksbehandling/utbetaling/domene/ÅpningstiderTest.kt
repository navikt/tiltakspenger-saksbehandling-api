import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider.erInnenforØkonomisystemetsÅpningstider
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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
        val klokkeslett = LocalTime.of(time, minutt)
        val clock = lagClock(dato, klokkeslett)
        assertEquals(
            forventetÅpent,
            erInnenforØkonomisystemetsÅpningstider(clock),
            "forventetÅpent=$forventetÅpent for ${dato.dayOfWeek} $klokkeslett",
        )
    }

    private fun lagClock(dato: LocalDate, tid: LocalTime): Clock {
        val zoned = ZonedDateTime.of(dato, tid, ZoneId.systemDefault())
        return Clock.fixed(zoned.toInstant(), ZoneId.systemDefault())
    }
}
