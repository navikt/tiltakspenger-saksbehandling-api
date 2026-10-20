package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import org.junit.jupiter.api.Test

class UtelatteYtelsestyperTest {
    private val typerSomHentes = Ytelsetype.entries.map { it.tekstverdi }

    @Test
    fun `en type hentes eller er utelatt, aldri begge`() {
        typerSomHentes.filter { it in utelatteYtelsestyper }.shouldBeEmpty()
    }

    @Test
    fun `typer som fratar rett hentes`() {
        typerSomHentes shouldContainAll listOf("Omsorgspenger", "Opplæringspenger", "Svangerskapspenger")
        utelatteYtelsestyper shouldNotContainAnyOf listOf("Omsorgspenger", "Opplæringspenger", "Svangerskapspenger")
    }

    @Test
    fun `barnepensjon og utdanningsstønad påvirker ikke tiltakspenger`() {
        typerSomHentes shouldNotContainAnyOf listOf("Barnepensjon", "Utdanningsstønad")
        utelatteYtelsestyper shouldContainAll listOf("Barnepensjon", "Utdanningsstønad")
    }
}
