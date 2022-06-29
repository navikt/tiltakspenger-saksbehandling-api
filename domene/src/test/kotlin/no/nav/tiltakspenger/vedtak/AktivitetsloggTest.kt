package no.nav.tiltakspenger.vedtak


import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AktivitetsloggTest {

    private lateinit var aktivitetslogg: Aktivitetslogg
    private lateinit var person: TestKontekst

    @BeforeEach
    fun setUp() {
        person = TestKontekst("Søker")
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `inneholder original melding`() {
        val infomelding = "info message"
        aktivitetslogg.info(infomelding)
        assertInfo(infomelding)
    }

    @Test
    fun `har ingen feil ved default`() {
        assertFalse(aktivitetslogg.hasErrors())
    }

    @Test
    fun `severe oppdaget og kaster exception`() {
        val melding = "Severe error"
        assertThrows<Aktivitetslogg.AktivitetException> { aktivitetslogg.severe(melding) }
        assertTrue(aktivitetslogg.hasErrors())
        assertTrue(aktivitetslogg.toString().contains(melding))
        assertSevere(melding)
    }

    @Test
    fun `error oppdaget`() {
        val melding = "Error"
        aktivitetslogg.error(melding)
        assertTrue(aktivitetslogg.hasErrors())
        assertTrue(aktivitetslogg.toString().contains(melding))
        assertError(melding)
    }

    @Test
    fun `warning oppdaget`() {
        val melding = "Warning explanation"
        aktivitetslogg.warn(melding)
        assertFalse(aktivitetslogg.hasErrors())
        assertTrue(aktivitetslogg.toString().contains(melding))
        assertWarn(melding)
    }

    @Test
    fun `Melding sendt til forelder`() {
        val hendelse = TestHendelse(
            "Hendelse",
            aktivitetslogg.barn()
        )
        "info message".also {
            hendelse.info(it)
            assertInfo(it, hendelse.logg)
            assertInfo(it, aktivitetslogg)
        }
        "error message".also {
            hendelse.error(it)
            assertError(it, hendelse.logg)
            assertError(it, aktivitetslogg)
        }
    }

    @Test
    fun `Melding sendt fra barnebarn til forelder`() {
        val hendelse = TestHendelse(
            "Hendelse",
            aktivitetslogg.barn()
        )
        hendelse.kontekst(person)
        val arbeidsgiver =
            TestKontekst("Melding")
        hendelse.kontekst(arbeidsgiver)
        val vedtaksperiode =
            TestKontekst("Soknad")
        hendelse.kontekst(vedtaksperiode)
        "info message".also {
            hendelse.info(it)
            assertInfo(it, hendelse.logg)
            assertInfo(it, aktivitetslogg)
        }
        "error message".also {
            hendelse.error(it)
            assertError(it, hendelse.logg)
            assertError(it, aktivitetslogg)
            assertError("Hendelse", aktivitetslogg)
            assertError("Soknad", aktivitetslogg)
            assertError("Melding", aktivitetslogg)
            assertError("Søker", aktivitetslogg)
        }
    }

    @Test
    fun `Vis bare arbeidsgiveraktivitet`() {
        val hendelse1 = TestHendelse(
            "Hendelse1",
            aktivitetslogg.barn()
        )
        hendelse1.kontekst(person)
        val arbeidsgiver1 =
            TestKontekst("Arbeidsgiver 1")
        hendelse1.kontekst(arbeidsgiver1)
        val vedtaksperiode1 =
            TestKontekst("Vedtaksperiode 1")
        hendelse1.kontekst(vedtaksperiode1)
        hendelse1.info("info message")
        hendelse1.warn("warn message")
        hendelse1.error("error message")
        val hendelse2 = TestHendelse(
            "Hendelse2",
            aktivitetslogg.barn()
        )
        hendelse2.kontekst(person)
        val arbeidsgiver2 =
            TestKontekst("Arbeidsgiver 2")
        hendelse2.kontekst(arbeidsgiver2)
        val vedtaksperiode2 =
            TestKontekst("Vedtaksperiode 2")
        hendelse2.kontekst(vedtaksperiode2)
        hendelse2.info("info message")
        hendelse2.error("error message")
        assertEquals(5, aktivitetslogg.aktivitetsteller())
        assertEquals(3, aktivitetslogg.logg(vedtaksperiode1).aktivitetsteller())
        assertEquals(2, aktivitetslogg.logg(arbeidsgiver2).aktivitetsteller())
    }

    @Test
    fun `Behov kan ha detaljer`() {
        val hendelse1 = TestHendelse(
            "Hendelse1",
            aktivitetslogg.barn()
        )
        hendelse1.kontekst(person)
        val param1 = "value"
        val param2 = LocalDate.now()
        hendelse1.behov(
            Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata,
            "Trenger persondata",
            mapOf(
                "param1" to param1,
                "param2" to param2
            )
        )

        assertEquals(1, aktivitetslogg.behov().size)
        assertEquals(1, aktivitetslogg.behov().first().kontekst().size)
        assertEquals(2, aktivitetslogg.behov().first().detaljer().size)
        assertEquals("Søker", aktivitetslogg.behov().first().kontekst()["Søker"])
        assertEquals(param1, aktivitetslogg.behov().first().detaljer()["param1"])
        assertEquals(param2, aktivitetslogg.behov().first().detaljer()["param2"])
    }

    private fun assertInfo(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(
            object : AktivitetsloggVisitor {
                override fun visitInfo(
                    kontekster: List<SpesifikkKontekst>,
                    aktivitet: Aktivitetslogg.Aktivitet.Info,
                    melding: String,
                    tidsstempel: String
                ) {
                    visitorCalled = true
                    assertEquals(message, melding)
                }
            }
        )
        assertTrue(visitorCalled)
    }

    private fun assertWarn(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(
            object : AktivitetsloggVisitor {
                override fun visitWarn(
                    kontekster: List<SpesifikkKontekst>,
                    aktivitet: Aktivitetslogg.Aktivitet.Warn,
                    melding: String,
                    tidsstempel: String
                ) {
                    visitorCalled = true
                    assertEquals(message, melding)
                }
            }
        )
        assertTrue(visitorCalled)
    }

    private fun assertError(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(
            object : AktivitetsloggVisitor {
                override fun visitError(
                    kontekster: List<SpesifikkKontekst>,
                    aktivitet: Aktivitetslogg.Aktivitet.Error,
                    melding: String,
                    tidsstempel: String
                ) {
                    visitorCalled = true
                    assertTrue(message in aktivitet.toString(), aktivitetslogg.toString())
                }
            }
        )
        assertTrue(visitorCalled)
    }

    private fun assertSevere(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(
            object : AktivitetsloggVisitor {
                override fun visitSevere(
                    kontekster: List<SpesifikkKontekst>,
                    aktivitet: Aktivitetslogg.Aktivitet.Severe,
                    melding: String,
                    tidsstempel: String
                ) {
                    visitorCalled = true
                    assertEquals(message, melding)
                }
            }
        )
        assertTrue(visitorCalled)
    }

    private class TestKontekst(
        private val melding: String
    ) : Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(melding, mapOf(melding to melding))
    }

    private class TestHendelse(
        private val melding: String,
        internal val logg: Aktivitetslogg
    ) : Aktivitetskontekst, IAktivitetslogg by logg {
        init {
            logg.kontekst(this)
        }

        override fun toSpesifikkKontekst() = SpesifikkKontekst("TestHendelse")
        override fun kontekst(kontekst: Aktivitetskontekst) {
            logg.kontekst(kontekst)
        }
    }
}