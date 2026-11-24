package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldeperiodeTest {

    private val kunHverdager = ObjectMother.meldeperiode(antallDagerForPeriode = 10)
    private val alleDager = kunHverdager.copy(girRett = kunHverdager.girRett.mapValues { true })

    @Test
    fun `har ikke flere dager med rett eller nytt antall dager når retten er uendret`() {
        kunHverdager.copy(versjon = kunHverdager.versjon.inc())
            .harFlereDagerMedRettEllerNyttAntallDager(kunHverdager) shouldBe false
    }

    @Test
    fun `har flere dager med rett når en dag som ikke ga rett gir rett`() {
        alleDager.harFlereDagerMedRettEllerNyttAntallDager(kunHverdager) shouldBe true
    }

    @Test
    fun `har ikke flere dager med rett når bare dager som ga rett ikke gir rett lenger`() {
        kunHverdager.harFlereDagerMedRettEllerNyttAntallDager(alleDager) shouldBe false
    }

    @Test
    fun `har nytt antall dager både ved flere og færre dager`() {
        val færreDager = kunHverdager.copy(maksAntallDagerForMeldeperiode = 8)

        færreDager.harFlereDagerMedRettEllerNyttAntallDager(kunHverdager) shouldBe true
        kunHverdager.harFlereDagerMedRettEllerNyttAntallDager(færreDager) shouldBe true
    }
}
