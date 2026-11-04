package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.response.henteteYtelsestyper
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.response.tilYtelsetype
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.response.utelatteYtelsestyper
import org.junit.jupiter.api.Test

class UtelatteYtelsestyperTest {
    private val typerSomHentes = henteteYtelsestyper

    @Test
    fun `alle typene har en tekst fra sokos-utbetaldata, så en ny type ikke glemmes`() {
        typerSomHentes.map { it.tilYtelsetype() }.toSet() shouldBe Ytelsetype.entries.toSet()
    }

    @Test
    fun `begge stavemåtene av Statens pensjonskasse gir SPK`() {
        "Statens pensjonskasse".tilYtelsetype() shouldBe Ytelsetype.SPK
        "Statens pensjonkasse".tilYtelsetype() shouldBe Ytelsetype.SPK
    }

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
