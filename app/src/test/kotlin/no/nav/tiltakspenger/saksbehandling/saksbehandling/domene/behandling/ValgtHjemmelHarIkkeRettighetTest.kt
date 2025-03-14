package no.nav.tiltakspenger.saksbehandling.repository.behandling

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelForAvslag
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelHarIkkeRettighet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class ValgtHjemmelHarIkkeRettighetTest {

    @Test
    fun `deserialiserer ValgtHjemmelForStans`() {
        val json = """
            [
                {"kode": "DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK", "type": "STANS", "beskrivelse": "tiltakspengeforskriften § 2 - ingen deltagelse"}
            ]
        """.trimIndent()

        val result = json.toValgtHjemmelHarIkkeRettighet()

        assertEquals(1, result.size)
        assertEquals(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak, result[0])
    }

    @Test
    fun `deserialiserer ValgtHjemmelForAvslag`() {
        val json = """
            [
                {"kode": "ALDER", "type": "AVSLAG", "beskrivelse": "tiltakspengeforskriften § 3 - alder"}
            ]
        """.trimIndent()

        val result = json.toValgtHjemmelHarIkkeRettighet()

        assertEquals(1, result.size)
        assertEquals(ValgtHjemmelForAvslag.Alder, result[0])
    }

    @Test
    fun `deserialiserer flere ValgtHjemmelForAvslag`() {
        val json = """
            [
                {"kode": "ALDER", "type": "AVSLAG", "beskrivelse": "tiltakspengeforskriften § 3 - alder"},
                {"kode": "INSTITUSJONSOPPHOLD", "type": "AVSLAG", "beskrivelse": "tiltakspengeforskriften § 9 - institusjonsopphold"}
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
    fun `kaster exception om koden ikke finnes`() {
        val json = """
            [
                {"kode": "DENNE_KODEN_FINNES_IKKE", "type": "STANS", "beskrivelse": "beskrivelse"}
            ]
        """.trimIndent()

        assertThrows<NoSuchElementException> {
            json.toValgtHjemmelHarIkkeRettighet()
        }
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
            [{"kode":"DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK","type":"STANS","beskrivelse":"tiltakspengeforskriften § 2 - ingen deltagelse"}]
        """.trimIndent()

        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.LENIENT)
    }

    @Test
    fun `serialiserer liste med ValgtHjemmelForAvslag`() {
        val list = listOf(ValgtHjemmelForAvslag.Alder)
        val json = list.toDbJson()
        val expectedJson = """
            [{"kode":"ALDER","type":"AVSLAG","beskrivelse":"tiltakspengeforskriften § 3 - alder"}]
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
            [{"kode":"DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK","type":"AVSLAG","beskrivelse":"tiltakspengeforskriften § 2 - ingen deltagelse"},
             {"kode":"ALDER","type":"AVSLAG","beskrivelse":"tiltakspengeforskriften § 3 - alder"}]
        """.trimIndent()

        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.LENIENT)
    }
}
