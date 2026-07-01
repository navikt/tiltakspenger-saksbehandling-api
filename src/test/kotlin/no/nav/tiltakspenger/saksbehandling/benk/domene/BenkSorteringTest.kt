package no.nav.tiltakspenger.saksbehandling.benk.domene

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BenkSorteringTest {
    @Test
    fun `skal parse gyldig sorteringsstreng`() {
        val sortering = BenkSortering.fromString("startet,DESC")
        sortering.kolonne shouldBe BenkSorteringKolonne.STARTET
        sortering.retning shouldBe SorteringRetning.DESC
    }

    @Test
    fun `skal bruke standardverdier når verdier mangler`() {
        val sortering = BenkSortering.fromString("")
        sortering.kolonne shouldBe BenkSorteringKolonne.STARTET
        sortering.retning shouldBe SorteringRetning.ASC
    }

    @Test
    fun `skal være case-insensitiv`() {
        val sortering = BenkSortering.fromString("sist_endret,asc")
        sortering.kolonne shouldBe BenkSorteringKolonne.SIST_ENDRET
        sortering.retning shouldBe SorteringRetning.ASC
    }
}
