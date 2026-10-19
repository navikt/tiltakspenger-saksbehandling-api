package no.nav.tiltakspenger.saksbehandling.felles

import io.github.oshai.kotlinlogging.Level
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.Loggfanger
import no.nav.tiltakspenger.saksbehandling.common.Sikkerloggfanger
import org.junit.jupiter.api.Test

class LoggbarTest {

    private data class Testfeil(
        override val loggkontekst: Loggkontekst,
        override val sikkerloggkontekst: Loggkontekst? = null,
    ) : Loggbar

    @Test
    fun `feil uten sikkerloggkontekst gir én error-linje og ingenting i sikkerlogg`() {
        val loggfanger = Loggfanger("LoggbarTest")
        val sikkerloggfanger = Sikkerloggfanger()
        val underliggende = IllegalStateException("uten sensitivt innhold")

        Testfeil(Loggkontekst("Feil ved testoperasjon", underliggende)).loggFeil(loggfanger, sikkerloggfanger, "sakId=1")

        val linje = loggfanger.logglinjer.single()
        linje.nivå shouldBe Level.ERROR
        linje.melding shouldBe "Feil ved testoperasjon. sakId=1."
        linje.feil shouldBe underliggende
        sikkerloggfanger.sikkerlogglinjer.shouldBeEmpty()
    }

    @Test
    fun `feil med sikkerloggkontekst gir én error-linje som viser til sikkerlogg, og én error-linje der`() {
        val loggfanger = Loggfanger("LoggbarTest")
        val sikkerloggfanger = Sikkerloggfanger(seSikkerlogg = "Se sikkerlogg.")
        val underliggende = IllegalStateException("med sensitivt innhold")

        Testfeil(
            loggkontekst = Loggkontekst("Feil ved testoperasjon"),
            sikkerloggkontekst = Loggkontekst("Feil ved testoperasjon. Response: sensitiv", underliggende),
        ).loggFeil(loggfanger, sikkerloggfanger, "sakId=1")

        val linje = loggfanger.logglinjer.single()
        linje.nivå shouldBe Level.ERROR
        linje.melding shouldBe "Feil ved testoperasjon. sakId=1. Se sikkerlogg."
        linje.feil shouldBe null
        val sikkerlogglinje = sikkerloggfanger.sikkerlogglinjer.single()
        sikkerlogglinje.nivå shouldBe Sikkerloggfanger.Nivå.ERROR
        sikkerlogglinje.melding shouldBe "Feil ved testoperasjon. Response: sensitiv. sakId=1."
        sikkerlogglinje.feil shouldBe underliggende
    }
}
