package no.nav.tiltakspenger.saksbehandling.benk.domene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BenkSorteringTest {
    @Test
    fun `skal parse gyldig sorteringsstreng`() {
        val sortering = BenkSortering.fromString("startet,DESC")
        assertEquals(BenkSorteringKolonne.STARTET, sortering.kolonne)
        assertEquals(SorteringRetning.DESC, sortering.retning)
    }

    @Test
    fun `skal bruke standardverdier når verdier mangler`() {
        val sortering = BenkSortering.fromString("")
        assertEquals(BenkSorteringKolonne.STARTET, sortering.kolonne)
        assertEquals(SorteringRetning.ASC, sortering.retning)
    }

    @Test
    fun `skal være case-insensitiv`() {
        val sortering = BenkSortering.fromString("sist_endret,asc")
        assertEquals(BenkSorteringKolonne.SIST_ENDRET, sortering.kolonne)
        assertEquals(SorteringRetning.ASC, sortering.retning)
    }
}
