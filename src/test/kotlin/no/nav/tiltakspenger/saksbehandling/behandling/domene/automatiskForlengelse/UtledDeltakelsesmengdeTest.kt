package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.Deltakelsestilstand
import org.junit.jupiter.api.Test

class UtledDeltakelsesmengdeTest {

    private fun deltakelsesmengde(antallDagerPerUke: Float?, deltakelseProsent: Float?) = Deltakelsestilstand(
        deltakelseFraOgMed = null,
        deltakelseTilOgMed = null,
        deltakelseStatus = TiltakDeltakerstatus.Deltar,
        deltakelseProsent = deltakelseProsent,
        antallDagerPerUke = antallDagerPerUke,
    ).utledDeltakelsesmengde()

    private fun UtledetDeltakelsesmengde?.skalGi(antallDagerPerMeldeperiode: Int, beskrivelse: String) {
        this shouldBe UtledetDeltakelsesmengde(AntallDagerForMeldeperiode(antallDagerPerMeldeperiode), beskrivelse)
    }

    @Test
    fun `dager per uke brukes når det er satt, selv om prosenten sier noe annet`() {
        deltakelsesmengde(antallDagerPerUke = 3F, deltakelseProsent = 100F).skalGi(6, "3 dager per uke")
    }

    @Test
    fun `halve dager per uke gir et helt antall dager per meldeperiode`() {
        deltakelsesmengde(antallDagerPerUke = 2.5F, deltakelseProsent = null).skalGi(5, "2,5 dager per uke")
    }

    @Test
    fun `prosent brukes når dager per uke ikke er satt, der 100 prosent er 5 dager per uke`() {
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 100F).skalGi(10, "100 prosent")
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 80F).skalGi(8, "80 prosent")
        deltakelsesmengde(antallDagerPerUke = 0F, deltakelseProsent = 50F).skalGi(5, "50 prosent")
    }

    @Test
    fun `prosent som ikke gir hele dager rundes til nærmeste hele dag per meldeperiode`() {
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 33.3F).skalGi(3, "33,3 prosent")
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 25F).skalGi(3, "25 prosent")
    }

    @Test
    fun `svært lav deltakelsesmengde gir minst 1 dag per meldeperiode`() {
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 2F).skalGi(1, "2 prosent")
    }

    @Test
    fun `kan ikke utledes uten dager per uke og prosent`() {
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = null).shouldBeNull()
        deltakelsesmengde(antallDagerPerUke = 0F, deltakelseProsent = 0F).shouldBeNull()
    }

    @Test
    fun `kan ikke utledes når deltakelsesmengden er mer enn fulltid`() {
        deltakelsesmengde(antallDagerPerUke = 5.5F, deltakelseProsent = null).shouldBeNull()
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 120F).shouldBeNull()
    }
}
