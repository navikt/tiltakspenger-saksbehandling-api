package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.json.objectMapper
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDate

class SladdbarVerdiTest {

    private data class TestDTO(
        val tekst: SladdbarVerdi<String?>,
        val dato: SladdbarVerdi<LocalDate>,
    )

    @Test
    fun `en verdi som ikke er sladdet serialiseres med verdien og erSladdet false`() {
        val json = objectMapper.writeValueAsString(
            TestDTO(tekst = "Hemmelig".ikkeSladdet(), dato = 1.januar(2025).ikkeSladdet()),
        )

        JSONAssert.assertEquals(
            """
            {
              "tekst": { "verdi": "Hemmelig", "erSladdet": false },
              "dato": { "verdi": "2025-01-01", "erSladdet": false }
            }
            """.trimIndent(),
            json,
            true,
        )
    }

    @Test
    fun `en verdi som mangler skiller seg fra en sladdet verdi`() {
        val json = objectMapper.writeValueAsString(
            TestDTO(tekst = null.ikkeSladdet(), dato = SladdetVerdi),
        )

        JSONAssert.assertEquals(
            """
            {
              "tekst": { "verdi": null, "erSladdet": false },
              "dato": { "verdi": null, "erSladdet": true }
            }
            """.trimIndent(),
            json,
            true,
        )
    }

    @Test
    fun `sladdet verdi kan brukes som enhver sladdbar verdi`() {
        val sladdet: SladdbarVerdi<LocalDate> = SladdetVerdi

        sladdet.verdi shouldBe null
        sladdet.erSladdet shouldBe true
        "Hemmelig".ikkeSladdet().verdi shouldBe "Hemmelig"
        "Hemmelig".ikkeSladdet().erSladdet shouldBe false
    }
}
