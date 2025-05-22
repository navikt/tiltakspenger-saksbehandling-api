package no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling

import io.kotest.assertions.throwables.shouldThrow
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.tilHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.toAvslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.toDb
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.toDbJson
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

        val result = json.tilHjemmelForStans()

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

        val result = json.toAvslagsgrunnlag()

        assertEquals(1, result.size)
        assertEquals(Avslagsgrunnlag.Alder, result.first())
    }

    @Test
    fun `deserialiserer flere ValgtHjemmelForStans`() {
        // Syntax highlighting
        // language=JSON
        val json = """
            [
              "STANS_ALDER", 
              "STANS_INSTITUSJONSOPPHOLD"
            ]
        """.trimIndent()

        val result = json.tilHjemmelForStans()

        assertEquals(2, result.size)
    }

    @Test
    fun `deserialisering tomt array for stans`() {
        val json = "[]"
        val resultStans = json.tilHjemmelForStans()

        assertTrue(resultStans.isEmpty())
    }

    @Test
    fun `skal ikke ha tomt array for avslag`() {
        val json = "[]"

        shouldThrow<NullPointerException> { json.toAvslagsgrunnlag() }
    }

    @Test
    fun `serialiserer tom liste`() {
        val list = emptyList<ValgtHjemmelForStans>()
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
    fun `serialiserer set med ValgtHjemmelForAvslag`() {
        val list = setOf(Avslagsgrunnlag.Alder)
        val json = list.toDb()
        val expectedJson = """
            [
              "AVSLAG_ALDER"
            ]
        """.trimIndent()

        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.LENIENT)
    }

    @Test
    fun `serialiserer liste med flere elementer`() {
        val list = setOf(
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak,
            Avslagsgrunnlag.Alder,
        )
        val json = list.toDb()
        val expectedJson = """
            [
              "AVSLAG_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK",
              "AVSLAG_ALDER"
            ]
        """.trimIndent()

        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.LENIENT)
    }
}
