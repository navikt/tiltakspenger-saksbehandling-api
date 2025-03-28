package no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling

import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.toValgtHjemmelHarIkkeRettighet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class ValgtHjemmelHarIkkeRettighetTest {

    @Test
    fun `deserialiserer ValgtHjemmelForStans`() {
        // Syntax highlighting
        // language=JSON
        val json = """
            [
                "STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK"
            ]
        """.trimIndent()

        val result = json.toValgtHjemmelHarIkkeRettighet()

        assertEquals(1, result.size)
        assertEquals(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak, result[0])
    }

    @Test
    fun `deserialiserer ValgtHjemmelForAvslag`() {
        // Syntax highlighting
        // language=JSON
        val json = """
            [
              "AVSLAG_ALDER"
            ]
        """.trimIndent()

        val result = json.toValgtHjemmelHarIkkeRettighet()

        assertEquals(1, result.size)
        assertEquals(ValgtHjemmelForAvslag.Alder, result[0])
    }

    @Test
    fun `deserialiserer flere ValgtHjemmelForAvslag`() {
        // Syntax highlighting
        // language=JSON
        val json = """
            [
              "STANS_ALDER", 
              "STANS_INSTITUSJONSOPPHOLD"
            ]
        """.trimIndent()

        val result = json.toValgtHjemmelHarIkkeRettighet()

        assertEquals(2, result.size)
    }

    @Test
    fun `deserialisering tomt array`() {
        val json = "[]"
        val result = json.toValgtHjemmelHarIkkeRettighet()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `serialiserer tom liste`() {
        val list = emptyList<ValgtHjemmelHarIkkeRettighet>()
        val json = list.toDbJson()
        val expectedJson = "[]"

        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.LENIENT)
    }

    @Test
    fun `serialiserer liste med ValgtHjemmelForStans`() {
        val list = listOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak)
        val json = list.toDbJson()
        val expectedJson = """
            [
              "STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK"
            ]
        """.trimIndent()

        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.LENIENT)
    }

    @Test
    fun `serialiserer liste med ValgtHjemmelForAvslag`() {
        val list = listOf(ValgtHjemmelForAvslag.Alder)
        val json = list.toDbJson()
        val expectedJson = """
            [
              "AVSLAG_ALDER"
            ]
        """.trimIndent()

        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.LENIENT)
    }

    @Test
    fun `serialiserer liste med flere elementer`() {
        val list = listOf(
            ValgtHjemmelForAvslag.DeltarIkkePåArbeidsmarkedstiltak,
            ValgtHjemmelForAvslag.Alder,
        )
        val json = list.toDbJson()
        val expectedJson = """
            [
              "AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK",
              "AVSLAG_ALDER"
            ]
        """.trimIndent()

        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.LENIENT)
    }
}
